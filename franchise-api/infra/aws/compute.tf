# One EC2 instance running the API container published to GHCR. It is managed
# through SSM Session Manager / Run Command, so no SSH key or port 22 is needed.

data "aws_ssm_parameter" "al2023_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "api" {
  name               = "${var.project_name}-api"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "api_ssm_core" {
  role       = aws_iam_role.api.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "api_read_db_secrets" {
  statement {
    actions = ["ssm:GetParameter"]
    resources = [
      aws_ssm_parameter.db_master_password.arn,
      aws_ssm_parameter.db_app_password.arn,
    ]
  }
}

resource "aws_iam_role_policy" "api_read_db_secrets" {
  name   = "read-db-secrets"
  role   = aws_iam_role.api.id
  policy = data.aws_iam_policy_document.api_read_db_secrets.json
}

resource "aws_iam_instance_profile" "api" {
  name = "${var.project_name}-api"
  role = aws_iam_role.api.name
}

resource "aws_instance" "api" {
  ami                    = data.aws_ssm_parameter.al2023_ami.value
  instance_type          = var.instance_type
  subnet_id              = sort(data.aws_subnets.api.ids)[0]
  vpc_security_group_ids = [aws_security_group.api.id]
  iam_instance_profile   = aws_iam_instance_profile.api.name

  associate_public_ip_address = true

  metadata_options {
    http_tokens = "required" # IMDSv2 only
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 16
    encrypted   = true
  }

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    region          = var.aws_region
    api_image       = var.api_image
    swagger_enabled = var.swagger_enabled
    db_host         = aws_db_instance.this.address
    db_port         = aws_db_instance.this.port
    db_name         = var.db_name
    db_master_user  = var.db_master_username
    db_app_user     = var.db_app_username
    master_pw_param = aws_ssm_parameter.db_master_password.name
    app_pw_param    = aws_ssm_parameter.db_app_password.name
  })
  user_data_replace_on_change = true

  # The AMI parameter moves with every Amazon Linux release; replacing the
  # instance for that alone would be an unplanned outage.
  lifecycle {
    ignore_changes = [ami]
  }

  tags = {
    Name = "${var.project_name}-api"
  }

  depends_on = [aws_iam_role_policy.api_read_db_secrets]
}

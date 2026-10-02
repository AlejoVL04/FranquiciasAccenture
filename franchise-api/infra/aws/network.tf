# The default VPC keeps the footprint (and the cost) minimal: no NAT gateway,
# no custom routing. The database is still private: it has no public IP and its
# security group only accepts connections from the API instance.

data "aws_vpc" "default" {
  default = true
}

# Availability zones that actually offer the chosen instance type (t3.micro is
# missing from some, e.g. us-east-1e).
data "aws_ec2_instance_type_offerings" "api" {
  location_type = "availability-zone"

  filter {
    name   = "instance-type"
    values = [var.instance_type]
  }
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

data "aws_subnets" "api" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }

  filter {
    name   = "availability-zone"
    values = data.aws_ec2_instance_type_offerings.api.locations
  }
}

resource "aws_security_group" "api" {
  name        = "${var.project_name}-api"
  description = "Franchise API: HTTP in, everything out"
  vpc_id      = data.aws_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "api_http" {
  for_each = toset(var.allowed_http_cidrs)

  security_group_id = aws_security_group.api.id
  description       = "HTTP to the API"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_egress_rule" "api_all" {
  security_group_id = aws_security_group.api.id
  description       = "Image pulls, SSM, RDS"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_security_group" "db" {
  name        = "${var.project_name}-db"
  description = "Franchise DB: MySQL from the API instance only"
  vpc_id      = data.aws_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "db_from_api" {
  security_group_id            = aws_security_group.db.id
  description                  = "MySQL from the API"
  ip_protocol                  = "tcp"
  from_port                    = 3306
  to_port                      = 3306
  referenced_security_group_id = aws_security_group.api.id
}

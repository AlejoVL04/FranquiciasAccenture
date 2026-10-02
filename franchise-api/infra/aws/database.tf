# Managed MySQL (RDS). Terraform only creates the empty database; tables,
# indexes and stored procedures are created by Flyway when the API starts,
# exactly as in the local and test environments.

# Alphanumeric only: the passwords travel through shell and SQL literals in
# the bootstrap script, where special characters would need escaping.
resource "random_password" "db_master" {
  length  = 32
  special = false
}

resource "random_password" "db_app" {
  length  = 32
  special = false
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.project_name}-db"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_db_parameter_group" "this" {
  name   = "${var.project_name}-mysql84"
  family = "mysql8.4"

  # Same server defaults as the Compose MySQL (see docker-compose.yml).
  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }

  parameter {
    name  = "collation_server"
    value = "utf8mb4_unicode_ci"
  }
}

resource "aws_db_instance" "this" {
  identifier = "${var.project_name}-db"

  engine               = "mysql"
  engine_version       = var.db_engine_version
  instance_class       = var.db_instance_class
  parameter_group_name = aws_db_parameter_group.this.name

  allocated_storage = var.db_allocated_storage
  storage_type      = "gp2"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_master_username
  password = random_password.db_master.result

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.db.id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period    = var.db_backup_retention_days
  auto_minor_version_upgrade = true
  apply_immediately          = true

  # Disposable environment for the technical test: `terraform destroy`
  # removes it without leaving a paid snapshot behind.
  deletion_protection = false
  skip_final_snapshot = true
}

# The instance reads the passwords at boot from Parameter Store (SecureString,
# encrypted with the AWS-managed key), so they never appear in user data.
resource "aws_ssm_parameter" "db_master_password" {
  name  = "/${var.project_name}/db/master-password"
  type  = "SecureString"
  value = random_password.db_master.result
}

resource "aws_ssm_parameter" "db_app_password" {
  name  = "/${var.project_name}/db/app-password"
  type  = "SecureString"
  value = random_password.db_app.result
}

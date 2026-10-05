variable "aws_region" {
  description = "AWS region where everything is created."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefix for resource names and the Project tag."
  type        = string
  default     = "franchise-api"
}

# --- API --------------------------------------------------------------------

variable "api_image" {
  description = "Container image of the API, published by the GitHub Actions pipeline."
  type        = string
  default     = "ghcr.io/alejovl04/franchise-api:latest"
}

variable "instance_type" {
  description = "EC2 instance type for the API. t3.micro is Free Tier eligible."
  type        = string
  default     = "t3.micro"
}

variable "allowed_http_cidrs" {
  description = "CIDR blocks allowed to reach the API on port 80."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "swagger_enabled" {
  description = "Expose Swagger UI and /v3/api-docs."
  type        = bool
  default     = true
}

# --- Database ---------------------------------------------------------------

variable "db_instance_class" {
  description = "RDS instance class. db.t3.micro is Free Tier eligible."
  type        = string
  default     = "db.t3.micro"
}

variable "db_engine_version" {
  description = "MySQL engine version on RDS (same major as the local and test setups)."
  type        = string
  default     = "8.4"
}

variable "db_allocated_storage" {
  description = "Storage in GiB (Free Tier covers 20)."
  type        = number
  default     = 20
}

variable "db_backup_retention_days" {
  description = "Automated backup retention in days (0 disables backups)."
  type        = number
  default     = 1
}

variable "db_name" {
  description = "Database (schema) created on the RDS instance."
  type        = string
  default     = "franchise_db"
}

variable "db_master_username" {
  description = "RDS master user. Only used once, to create the application user."
  type        = string
  default     = "franchise_admin"
}

variable "db_app_username" {
  description = "Least-privilege user the API connects with (same grants as database/setup.sql)."
  type        = string
  default     = "franchise_user"
}

output "api_url" {
  description = "Public base URL of the API."
  value       = "http://${aws_instance.api.public_dns}"
}

output "swagger_url" {
  description = "Swagger UI."
  value       = "http://${aws_instance.api.public_dns}/swagger-ui.html"
}

output "health_url" {
  description = "Health check."
  value       = "http://${aws_instance.api.public_dns}/actuator/health"
}

output "instance_id" {
  description = "EC2 instance running the API."
  value       = aws_instance.api.id
}

output "db_endpoint" {
  description = "RDS MySQL endpoint (private: reachable only from the API instance)."
  value       = aws_db_instance.this.endpoint
}

output "deploy_command" {
  description = "Pulls the latest image and restarts the API, without SSH."
  value       = "aws ssm send-command --region ${var.aws_region} --instance-ids ${aws_instance.api.id} --document-name AWS-RunShellScript --parameters commands=/usr/local/bin/franchise-api-deploy"
}

output "shell_command" {
  description = "Interactive shell on the instance (needs the Session Manager plugin)."
  value       = "aws ssm start-session --region ${var.aws_region} --target ${aws_instance.api.id}"
}

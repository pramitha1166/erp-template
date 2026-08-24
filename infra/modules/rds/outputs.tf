output "endpoint" {
  value = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}

output "secret_arn" {
  description = "Secrets Manager ARN holding username/password/dbname/host/port as JSON."
  value       = aws_secretsmanager_secret.db.arn
}

output "security_group_id" {
  value = aws_security_group.rds.id
}

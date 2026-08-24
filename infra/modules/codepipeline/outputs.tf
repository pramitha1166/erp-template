output "connection_arn" {
  description = "GitHub connection ARN. Starts PENDING — see infra/README.md for the one manual authorization step."
  value       = aws_codestarconnections_connection.github.arn
}

output "connection_status" {
  value = aws_codestarconnections_connection.github.connection_status
}

output "pipeline_name" {
  value = aws_codepipeline.this.name
}

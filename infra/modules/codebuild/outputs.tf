output "project_names" {
  description = "Map of app name (e.g. \"backend\") to its CodeBuild project name."
  value       = { for k, v in aws_codebuild_project.this : k => v.name }
}

output "project_arns" {
  value = { for k, v in aws_codebuild_project.this : k => v.arn }
}

output "service_role_name" {
  description = "The shared CodeBuild service role name — infra/modules/codepipeline grants it read access to the pipeline's artifact bucket."
  value       = aws_iam_role.codebuild.name
}

output "github_token_secret_arn" {
  description = "Secret to store a GitHub `deployments: write` token in; empty when status reporting is disabled."
  value       = try(aws_secretsmanager_secret.github_token[0].arn, "")
}

output "ddns_secret_arn" {
  description = "Secret holding the dynamic-DNS token and hostnames."
  value       = aws_secretsmanager_secret.ddns.arn
}

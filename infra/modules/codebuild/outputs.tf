output "source_bucket" {
  value = aws_s3_bucket.source.bucket
}

output "project_names" {
  description = "Map of app name (e.g. \"backend\") to its CodeBuild project name."
  value       = { for k, v in aws_codebuild_project.this : k => v.name }
}

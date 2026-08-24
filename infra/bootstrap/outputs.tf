output "state_bucket" {
  description = "S3 bucket to use as the backend for every environment under infra/environments (see backend.tf in each)."
  value       = aws_s3_bucket.tfstate.bucket
}

output "terraform_plan_role_arn" {
  description = "Set as the AWS_ROLE_ARN input for the terraform-plan job. No repo secret needed for OIDC — this ARN itself isn't sensitive."
  value       = aws_iam_role.terraform_plan.arn
}

output "terraform_apply_role_arn" {
  description = "Set as the AWS_ROLE_ARN input for the terraform-apply job, scoped to the \"staging\" GitHub Environment."
  value       = aws_iam_role.terraform_apply.arn
}

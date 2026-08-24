variable "aws_region" {
  type    = string
  default = "ap-south-1" # Mumbai — closest full-service AWS region to Sri Lanka.
}

variable "project" {
  type    = string
  default = "eudext-erp"
}

variable "environment" {
  type    = string
  default = "staging"
}

variable "github_repo" {
  description = "GitHub repository the pipeline sources from, as \"owner/repo\"."
  type        = string
  default     = "pramitha1166/erp-template"
}

variable "branch_name" {
  description = "Branch the pipeline builds from and deploys on every push to."
  type        = string
  # This repo has no "main" — claude/srs-review-breakdown-49ecvy is the
  # actual integration branch PRs merge into. Matches the branch filters in
  # .github/workflows/ci.yml and terraform.yml.
  default = "claude/srs-review-breakdown-49ecvy"
}

variable "certificate_arn" {
  description = "ACM certificate ARN for HTTPS on the ALB. Leave empty until a custom domain exists (BRD-4) — the environment serves plain HTTP on the ALB's own DNS name until then."
  type        = string
  default     = ""
}

variable "backend_image_tag" {
  description = "Floating tag the deploy pipeline pushes to and force-redeploys against."
  type        = string
  default     = "staging"
}

variable "frontend_image_tag" {
  type    = string
  default = "staging"
}

variable "rds_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "backend_desired_count" {
  type    = number
  default = 1
}

variable "frontend_desired_count" {
  type    = number
  default = 1
}

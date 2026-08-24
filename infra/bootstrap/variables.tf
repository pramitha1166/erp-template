variable "aws_region" {
  description = "AWS region for the state bucket and IAM (IAM is global, but the bucket needs a home region)."
  type        = string
  default     = "ap-south-1" # Mumbai — closest full-service AWS region to Sri Lanka.
}

variable "project" {
  description = "Short project slug used to prefix every resource name."
  type        = string
  default     = "eudext-erp"
}

variable "github_repo" {
  description = "GitHub repository the CI roles trust, as \"owner/repo\"."
  type        = string
  default     = "pramitha1166/erp-template"
}

variable "environment_name" {
  description = "GitHub Environment name used to gate the apply/deploy roles (must match the environment configured on the repo, e.g. protected with required reviewers)."
  type        = string
  default     = "staging"
}

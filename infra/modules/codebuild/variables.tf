variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "ecs_cluster_name" {
  type = string
}

variable "apps" {
  description = "One CodeBuild project per app. Key is the short app name (e.g. \"backend\", \"frontend\") — also the path to that app's buildspec.yml within the repo checkout (\"<app>/buildspec.yml\")."
  type = map(object({
    ecr_repository_url = string
    ecr_repository_arn = string
    ecs_service_name   = string
  }))
}

variable "tags" {
  type    = map(string)
  default = {}
}

variable "github_repo" {
  description = "Repository the deploy reports status to, as \"owner/repo\"."
  type        = string
  default     = ""
}

variable "app_base_url" {
  description = "Public URL of the deployed environment, linked from the GitHub deployment record."
  type        = string
  default     = ""
}

variable "enable_github_deployment_status" {
  description = <<-EOT
    Create the Secrets Manager secret holding a GitHub token and let each
    build report its outcome to the repo's Deployments API. The secret is
    created empty; deploys work normally until a token is stored in it.
  EOT
  type        = bool
  default     = true
}

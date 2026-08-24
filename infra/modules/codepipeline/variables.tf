variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "github_repo" {
  description = "GitHub repository the pipeline sources from, as \"owner/repo\"."
  type        = string
}

variable "branch_name" {
  type    = string
  default = "main"
}

variable "codebuild_project_names" {
  description = "Map of app name (e.g. \"backend\") to CodeBuild project name — one parallel Build action per entry."
  type        = map(string)
}

variable "codebuild_project_arns" {
  type = map(string)
}

variable "codebuild_service_role_name" {
  description = "The CodeBuild service role (infra/modules/codebuild) — granted read access to this pipeline's artifact bucket so its projects can pull the source CodePipeline hands them."
  type        = string
}

variable "tags" {
  type    = map(string)
  default = {}
}

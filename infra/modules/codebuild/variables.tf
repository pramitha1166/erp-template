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

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
  description = "One CodeBuild project per app. Key is the short app name (e.g. \"backend\", \"frontend\") — also the source zip's S3 key prefix and the buildspec.yml's location (\"<app>/buildspec.yml\" within the checked-out repo)."
  type = map(object({
    ecr_repository_url = string
    ecr_repository_arn = string
    ecs_service_name   = string
  }))
}

variable "source_zip_retention_days" {
  description = "How long uploaded source zips live in S3 before expiring. They're only needed for the duration of one build."
  type        = number
  default     = 7
}

variable "tags" {
  type    = map(string)
  default = {}
}

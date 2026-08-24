variable "project" {
  type = string
}

variable "repository_names" {
  description = "Short names (e.g. \"backend\", \"frontend\") — the actual ECR repo is named \"<project>-<name>\"."
  type        = list(string)
}

variable "image_retention_count" {
  description = "How many most-recent tagged images to keep per repository before older ones are expired."
  type        = number
  default     = 20
}

variable "tags" {
  type    = map(string)
  default = {}
}

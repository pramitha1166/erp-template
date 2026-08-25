variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "allowed_security_groups" {
  description = <<-EOT
    Security groups allowed to connect to Redis on 6379, keyed by a static
    name (e.g. `ecs_tasks`). A map rather than a list because the IDs are only
    known after apply, and `for_each` needs keys that are known at plan time.
  EOT
  type        = map(string)
}

variable "node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "tags" {
  type    = map(string)
  default = {}
}

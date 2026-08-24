variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "certificate_arn" {
  description = "ACM certificate ARN for an HTTPS listener. Leave empty to serve plain HTTP only (fine for a first bring-up against the ALB's own DNS name — see infra/README.md for adding a custom domain + TLS via BRD-4 once one exists)."
  type        = string
  default     = ""
}

variable "tags" {
  type    = map(string)
  default = {}
}

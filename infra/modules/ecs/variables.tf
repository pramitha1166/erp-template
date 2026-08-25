variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "task_subnet_ids" {
  description = "Subnets the Fargate tasks run in. Private subnets when the VPC has a NAT Gateway; otherwise the public subnets, paired with assign_task_public_ip."
  type        = list(string)
}

variable "assign_task_public_ip" {
  description = <<-EOT
    Give each task a public IP. Required when the tasks sit in public subnets,
    because Fargate needs egress to pull images from ECR and read secrets, and
    without NAT that route only exists via the Internet Gateway. Inbound is
    still closed: the tasks security group accepts traffic only from the ALB.
  EOT
  type        = bool
  default     = false
}

variable "alb_security_group_id" {
  type = string
}

variable "alb_listener_arn" {
  description = "Forces services to wait for the ALB listener to exist before attaching targets."
  type        = string
}

variable "frontend_target_group_arn" {
  type = string
}

variable "backend_target_group_arn" {
  type = string
}

variable "backend_image" {
  description = "Full image reference, e.g. \"<account>.dkr.ecr.<region>.amazonaws.com/eudext-erp-backend:staging\". The deploy pipeline owns what \"staging\" resolves to; Terraform never rewrites this tag."
  type        = string
}

variable "frontend_image" {
  type = string
}

variable "backend_cpu" {
  type    = number
  default = 512
}

variable "backend_memory" {
  type    = number
  default = 1024
}

variable "backend_desired_count" {
  type    = number
  default = 1
}

variable "frontend_cpu" {
  type    = number
  default = 256
}

variable "frontend_memory" {
  type    = number
  default = 512
}

variable "frontend_desired_count" {
  type    = number
  default = 1
}

variable "backend_health_check_grace_seconds" {
  description = "How long after a task starts before failed ALB health checks can kill it. Must exceed the app's cold-start time, measured at ~55s."
  type        = number
  default     = 180
}

variable "frontend_health_check_grace_seconds" {
  description = "As above, for the Next.js server — it serves almost immediately, so this only absorbs container-start jitter."
  type        = number
  default     = 60
}

variable "log_retention_days" {
  type    = number
  default = 30
}

variable "db_secret_arn" {
  type = string
}

variable "redis_host" {
  type = string
}

variable "redis_port" {
  type = number
}

variable "attachments_bucket_name" {
  type = string
}

variable "attachments_bucket_arn" {
  type = string
}

variable "public_base_url" {
  description = "Externally reachable base URL (ALB DNS name, or a custom domain once one exists) the frontend uses to reach the backend API, e.g. \"http://eudext-erp-staging-123.ap-south-1.elb.amazonaws.com\"."
  type        = string
}

variable "tags" {
  type    = map(string)
  default = {}
}

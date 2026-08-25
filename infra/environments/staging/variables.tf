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

variable "enable_nat_gateway" {
  description = <<-EOT
    Off by default here: a NAT Gateway plus its Elastic IP costs roughly
    USD 35/month and is not free-tier eligible. With it off the ECS tasks move
    into the public subnets with public IPs (inbound still ALB-only), and RDS
    and Redis stay in the private subnets. Turn it on for production, where
    tasks belong on private addresses.
  EOT
  type        = bool
  default     = false
}

variable "enable_alb" {
  description = <<-EOT
    Put an Application Load Balancer in front of the services. Off by
    default: it costs about USD 17/month plus its public IPv4 addresses, and
    it bills whether or not anything is deployed behind it. With it off the
    two services are reached directly on their tasks' public IPs — which
    change on every deployment, and carry no TLS. Turn it on for anything
    resembling production, or as soon as a stable URL matters.
  EOT
  type        = bool
  default     = false
}

variable "enable_redis" {
  description = <<-EOT
    Deploy ElastiCache. Off by default: it costs about USD 15/month and no
    application code reads from it yet — only the test suite, which uses its
    own Testcontainers Redis. Turn it on in the same change that introduces
    the first real use.
  EOT
  type        = bool
  default     = false
}

variable "rds_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "rds_backup_retention_days" {
  description = <<-EOT
    Automated-backup retention. Defaults to 1 because AWS free-tier accounts
    reject anything longer (FreeTierRestrictionError on CreateDBInstance).
    Raise this to 7+ once the account is off the free plan, and before this
    environment holds data anyone depends on.
  EOT
  type        = number
  default     = 1
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

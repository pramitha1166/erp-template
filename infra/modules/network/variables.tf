variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC. Public and private subnet CIDRs are derived from this."
  type        = string
  default     = "10.20.0.0/16"
}

variable "az_count" {
  description = "Number of availability zones to spread subnets across."
  type        = number
  default     = 2
}

variable "enable_nat_gateway" {
  description = <<-EOT
    Create NAT Gateways (and the Elastic IPs they need) so private subnets can
    reach the internet. Both are billed hourly and neither is free-tier
    eligible, so a cost-sensitive environment sets this to false — in which
    case workloads needing egress (ECS tasks pulling images from ECR, reaching
    Secrets Manager) must run in the public subnets with a public IP.
  EOT
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Use one NAT Gateway shared by all private subnets instead of one per AZ. Cheaper, at the cost of cross-AZ egress traffic and a single point of failure for outbound internet — an acceptable trade-off for a non-production environment."
  type        = bool
  default     = true
}

variable "tags" {
  type    = map(string)
  default = {}
}

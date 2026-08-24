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

variable "allowed_security_group_ids" {
  description = "Security groups (e.g. the ECS tasks SG) allowed to connect to Postgres on 5432."
  type        = list(string)
}

variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "allocated_storage_gb" {
  type    = number
  default = 20
}

variable "multi_az" {
  description = "Multi-AZ standby — leave false for a non-production environment to halve RDS cost."
  type        = bool
  default     = false
}

variable "backup_retention_days" {
  type    = number
  default = 7
}

variable "deletion_protection" {
  type    = bool
  default = false
}

variable "db_name" {
  type    = string
  default = "eudext_erp"
}

variable "db_username" {
  type    = string
  default = "eudext"
}

variable "tags" {
  type    = map(string)
  default = {}
}

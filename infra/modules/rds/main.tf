locals {
  name        = "${var.project}-${var.environment}"
  common_tags = merge(var.tags, { ManagedBy = "terraform" })
}

resource "aws_db_subnet_group" "this" {
  name       = local.name
  subnet_ids = var.private_subnet_ids
  tags       = local.common_tags
}

resource "aws_security_group" "rds" {
  name        = "${local.name}-rds"
  description = "Postgres access for ${local.name}"
  vpc_id      = var.vpc_id
  tags        = merge(local.common_tags, { Name = "${local.name}-rds" })
}

resource "aws_vpc_security_group_ingress_rule" "postgres_from_app" {
  for_each                     = toset(var.allowed_security_group_ids)
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = each.value
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
  description                  = "Postgres from application tier"
}

resource "aws_vpc_security_group_egress_rule" "all_outbound" {
  security_group_id = aws_security_group.rds.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "random_password" "db" {
  length  = 32
  special = false # simplifies safe interpolation into JDBC URLs and shell env
}

# ARCH-2/ARCH-7-adjacent: credentials never touch a .tfvars file or plain
# environment variable — Secrets Manager is the single source of truth, and
# the ECS task definition (infra/modules/ecs) injects it via `secrets`,
# never `environment`.
resource "aws_secretsmanager_secret" "db" {
  name = "${local.name}/db"
  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db.result
    dbname   = var.db_name
    host     = aws_db_instance.this.address
    port     = 5432
    # Precomputed so the ECS task definition can inject this one key
    # straight into DB_URL — ECS's secrets mechanism can pull a single JSON
    # key per environment variable, but can't concatenate the others.
    url = "jdbc:postgresql://${aws_db_instance.this.address}:5432/${var.db_name}"
  })
}

resource "aws_db_instance" "this" {
  identifier     = local.name
  engine         = "postgres"
  engine_version = "16"

  instance_class         = var.instance_class
  allocated_storage      = var.allocated_storage_gb
  storage_type           = "gp3"
  storage_encrypted      = true
  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result

  multi_az                   = var.multi_az
  backup_retention_period    = var.backup_retention_days
  deletion_protection        = var.deletion_protection
  skip_final_snapshot        = !var.deletion_protection
  final_snapshot_identifier  = var.deletion_protection ? "${local.name}-final" : null
  publicly_accessible        = false
  auto_minor_version_upgrade = true

  # ARCH-7: Flyway owns the schema exclusively — RDS's own auto-upgrade
  # mechanisms here only ever touch engine minor versions, never schema.

  tags = local.common_tags
}

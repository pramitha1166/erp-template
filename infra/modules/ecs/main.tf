locals {
  name        = "${var.project}-${var.environment}"
  common_tags = merge(var.tags, { ManagedBy = "terraform" })
}

resource "aws_ecs_cluster" "this" {
  name = local.name

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# Networking: tasks run in private subnets, reachable only from the ALB.
# ---------------------------------------------------------------------------

resource "aws_security_group" "tasks" {
  name        = "${local.name}-tasks"
  description = "ECS Fargate tasks for ${local.name}"
  vpc_id      = var.vpc_id
  tags        = merge(local.common_tags, { Name = "${local.name}-tasks" })
}

resource "aws_vpc_security_group_ingress_rule" "from_alb_backend" {
  security_group_id            = aws_security_group.tasks.id
  referenced_security_group_id = var.alb_security_group_id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
  description                  = "Backend from ALB"
}

resource "aws_vpc_security_group_ingress_rule" "from_alb_frontend" {
  security_group_id            = aws_security_group.tasks.id
  referenced_security_group_id = var.alb_security_group_id
  from_port                    = 3000
  to_port                      = 3000
  ip_protocol                  = "tcp"
  description                  = "Frontend from ALB"
}

resource "aws_vpc_security_group_egress_rule" "all_outbound" {
  security_group_id = aws_security_group.tasks.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# ---------------------------------------------------------------------------
# IAM: execution role (pull image, write logs, read secrets — used by the
# ECS agent itself) vs. task role (the backend app's own AWS SDK calls).
# ---------------------------------------------------------------------------

data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "${var.project}-${var.environment}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
  tags               = local.common_tags
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "execution_secrets" {
  statement {
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [var.db_secret_arn]
  }
}

resource "aws_iam_role_policy" "execution_secrets" {
  name   = "read-db-secret"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution_secrets.json
}

resource "aws_iam_role" "backend_task" {
  name               = "${var.project}-${var.environment}-backend-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
  tags               = local.common_tags
}

# DOC-1: the backend reads/writes attachments directly; scoped to its own
# bucket only.
data "aws_iam_policy_document" "backend_s3" {
  statement {
    effect    = "Allow"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${var.attachments_bucket_arn}/*"]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [var.attachments_bucket_arn]
  }
}

resource "aws_iam_role_policy" "backend_s3" {
  name   = "attachments-bucket"
  role   = aws_iam_role.backend_task.id
  policy = data.aws_iam_policy_document.backend_s3.json
}

# ---------------------------------------------------------------------------
# Logs
# ---------------------------------------------------------------------------

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${local.name}/backend"
  retention_in_days = var.log_retention_days
  tags              = local.common_tags
}

resource "aws_cloudwatch_log_group" "frontend" {
  name              = "/ecs/${local.name}/frontend"
  retention_in_days = var.log_retention_days
  tags              = local.common_tags
}

# ---------------------------------------------------------------------------
# Backend task + service
# ---------------------------------------------------------------------------

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.backend_cpu
  memory                   = var.backend_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.backend_task.arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = var.backend_image
      essential = true
      portMappings = [
        { containerPort = 8080, protocol = "tcp" }
      ]
      environment = [
        { name = "REDIS_HOST", value = var.redis_host },
        { name = "REDIS_PORT", value = tostring(var.redis_port) },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "ATTACHMENTS_BUCKET", value = var.attachments_bucket_name },
      ]
      secrets = [
        { name = "DB_URL", valueFrom = "${var.db_secret_arn}:url::" },
        { name = "DB_USERNAME", valueFrom = "${var.db_secret_arn}:username::" },
        { name = "DB_PASSWORD", valueFrom = "${var.db_secret_arn}:password::" },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.backend.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_service" "backend" {
  name            = "${local.name}-backend"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [aws_security_group.tasks.id]
  }

  load_balancer {
    target_group_arn = var.backend_target_group_arn
    container_name   = "backend"
    container_port   = 8080
  }

  # The deploy pipeline rolls new images with `--force-new-deployment`
  # against the ":staging" tag rather than by registering new task-def
  # revisions, so Terraform never fights it over which revision is "current".
  lifecycle {
    ignore_changes = [task_definition]
  }

  depends_on = [var.alb_listener_arn]
  tags       = local.common_tags
}

# ---------------------------------------------------------------------------
# Frontend task + service
# ---------------------------------------------------------------------------

resource "aws_ecs_task_definition" "frontend" {
  family                   = "${local.name}-frontend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.frontend_cpu
  memory                   = var.frontend_memory
  execution_role_arn       = aws_iam_role.execution.arn

  container_definitions = jsonencode([
    {
      name      = "frontend"
      image     = var.frontend_image
      essential = true
      portMappings = [
        { containerPort = 3000, protocol = "tcp" }
      ]
      environment = [
        { name = "NODE_ENV", value = "production" },
        { name = "NEXT_PUBLIC_API_BASE_URL", value = "${var.public_base_url}/api" },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.frontend.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_service" "frontend" {
  name            = "${local.name}-frontend"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = var.frontend_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [aws_security_group.tasks.id]
  }

  load_balancer {
    target_group_arn = var.frontend_target_group_arn
    container_name   = "frontend"
    container_port   = 3000
  }

  lifecycle {
    ignore_changes = [task_definition]
  }

  depends_on = [var.alb_listener_arn]
  tags       = local.common_tags
}

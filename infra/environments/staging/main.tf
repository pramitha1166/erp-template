module "network" {
  source = "../../modules/network"

  project     = var.project
  environment = var.environment

  enable_nat_gateway = var.enable_nat_gateway
}

module "ecr" {
  source = "../../modules/ecr"

  project          = var.project
  repository_names = ["backend", "frontend"]
}

module "storage" {
  source = "../../modules/storage"

  project     = var.project
  environment = var.environment
}

module "alb" {
  source = "../../modules/alb"

  project           = var.project
  environment       = var.environment
  vpc_id            = module.network.vpc_id
  public_subnet_ids = module.network.public_subnet_ids
  certificate_arn   = var.certificate_arn
}

module "rds" {
  source = "../../modules/rds"

  project               = var.project
  environment           = var.environment
  vpc_id                = module.network.vpc_id
  private_subnet_ids    = module.network.private_subnet_ids
  instance_class        = var.rds_instance_class
  backup_retention_days = var.rds_backup_retention_days

  # Not a cycle despite module.ecs also depending on module.rds below:
  # Terraform's dependency graph is per-resource, not per-module. This SG
  # ingress rule depends only on the ECS tasks security group *resource*
  # (created with no inputs of its own); the RDS instance and its secret
  # depend only on this module's own SG, never on anything in module.ecs.
  allowed_security_groups = { ecs_tasks = module.ecs.tasks_security_group_id }
}

module "redis" {
  source = "../../modules/redis"

  project                 = var.project
  environment             = var.environment
  vpc_id                  = module.network.vpc_id
  private_subnet_ids      = module.network.private_subnet_ids
  node_type               = var.redis_node_type
  allowed_security_groups = { ecs_tasks = module.ecs.tasks_security_group_id }
}

module "ecs" {
  source = "../../modules/ecs"

  project     = var.project
  environment = var.environment
  aws_region  = var.aws_region

  vpc_id = module.network.vpc_id

  # Without a NAT Gateway the private subnets have no egress, so the tasks run
  # in the public subnets with a public IP instead. Their security group still
  # admits nothing but the ALB either way.
  task_subnet_ids       = var.enable_nat_gateway ? module.network.private_subnet_ids : module.network.public_subnet_ids
  assign_task_public_ip = !var.enable_nat_gateway

  alb_security_group_id     = module.alb.security_group_id
  alb_listener_arn          = module.alb.listener_arn
  frontend_target_group_arn = module.alb.frontend_target_group_arn
  backend_target_group_arn  = module.alb.backend_target_group_arn
  public_base_url           = "http://${module.alb.dns_name}"

  backend_image  = "${module.ecr.repository_urls["backend"]}:${var.backend_image_tag}"
  frontend_image = "${module.ecr.repository_urls["frontend"]}:${var.frontend_image_tag}"

  backend_desired_count  = var.backend_desired_count
  frontend_desired_count = var.frontend_desired_count

  db_secret_arn = module.rds.secret_arn
  redis_host    = module.redis.endpoint
  redis_port    = module.redis.port

  attachments_bucket_name = module.storage.bucket_name
  attachments_bucket_arn  = module.storage.bucket_arn
}

module "codebuild" {
  source = "../../modules/codebuild"

  project          = var.project
  environment      = var.environment
  aws_region       = var.aws_region
  ecs_cluster_name = module.ecs.cluster_name

  apps = {
    backend = {
      ecr_repository_url = module.ecr.repository_urls["backend"]
      ecr_repository_arn = module.ecr.repository_arns["backend"]
      ecs_service_name   = module.ecs.backend_service_name
    }
    frontend = {
      ecr_repository_url = module.ecr.repository_urls["frontend"]
      ecr_repository_arn = module.ecr.repository_arns["frontend"]
      ecs_service_name   = module.ecs.frontend_service_name
    }
  }
}

module "codepipeline" {
  source = "../../modules/codepipeline"

  project     = var.project
  environment = var.environment
  aws_region  = var.aws_region
  github_repo = var.github_repo
  branch_name = var.branch_name

  codebuild_project_names     = module.codebuild.project_names
  codebuild_project_arns      = module.codebuild.project_arns
  codebuild_service_role_name = module.codebuild.service_role_name
}

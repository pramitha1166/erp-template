output "alb_dns_name" {
  description = "Public URL for the environment, empty when enable_alb is false (the tasks are then reached on their own public IPs — `./infra/scripts/env.sh status`)."
  value       = var.enable_alb ? module.alb[0].dns_name : ""
}

output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_backend_service_name" {
  value = module.ecs.backend_service_name
}

output "ecs_frontend_service_name" {
  value = module.ecs.frontend_service_name
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "attachments_bucket_name" {
  value = module.storage.bucket_name
}

output "codebuild_project_names" {
  value = module.codebuild.project_names
}

output "codepipeline_connection_arn" {
  description = "GitHub connection ARN — starts PENDING. Authorize it once in the AWS Console (Developer Tools > Connections) after apply; see infra/README.md."
  value       = module.codepipeline.connection_arn
}

output "codepipeline_connection_status" {
  value = module.codepipeline.connection_status
}

output "codepipeline_name" {
  value = module.codepipeline.pipeline_name
}

output "github_deployment_token_secret_arn" {
  description = "Store a GitHub token with `deployments: write` here to get deploy status reported on the repo."
  value       = module.codebuild.github_token_secret_arn
}

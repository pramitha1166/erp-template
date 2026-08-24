output "alb_dns_name" {
  description = "Public URL for the environment (http:// until certificate_arn is set)."
  value       = module.alb.dns_name
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

output "codebuild_source_bucket" {
  description = "Where .github/workflows/deploy.yml uploads source zips before triggering a build."
  value       = module.codebuild.source_bucket
}

output "codebuild_project_names" {
  value = module.codebuild.project_names
}

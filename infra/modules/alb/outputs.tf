output "dns_name" {
  value = aws_lb.this.dns_name
}

output "security_group_id" {
  value = aws_security_group.alb.id
}

output "frontend_target_group_arn" {
  value = aws_lb_target_group.frontend.arn
}

output "backend_target_group_arn" {
  value = aws_lb_target_group.backend.arn
}

output "listener_arn" {
  description = "The listener (HTTP or HTTPS, whichever is primary) that ECS services depend on before attaching to a target group."
  value       = local.has_tls ? aws_lb_listener.https[0].arn : aws_lb_listener.http.arn
}

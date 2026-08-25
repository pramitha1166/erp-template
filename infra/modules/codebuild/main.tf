locals {
  name        = "${var.project}-${var.environment}"
  common_tags = merge(var.tags, { ManagedBy = "terraform" })
}

data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------
# CodeBuild service role — one role shared by both projects, permissions
# scoped per-app via resource ARNs.
# ---------------------------------------------------------------------------

data "aws_iam_policy_document" "codebuild_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codebuild.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "codebuild" {
  name               = "${local.name}-codebuild"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume.json
  tags               = local.common_tags
}

data "aws_iam_policy_document" "codebuild" {
  statement {
    sid       = "EcrAuth"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid    = "EcrPush"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:PutImage",
      "ecr:BatchGetImage",
    ]
    resources = [for app in var.apps : app.ecr_repository_arn]
  }

  statement {
    sid    = "EcsDeploy"
    effect = "Allow"
    actions = [
      "ecs:UpdateService",
      "ecs:DescribeServices",
    ]
    resources = [
      for app in var.apps :
      "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:service/${var.ecs_cluster_name}/${app.ecs_service_name}"
    ]
  }

  dynamic "statement" {
    for_each = aws_secretsmanager_secret.github_token
    content {
      sid       = "ReadGithubToken"
      effect    = "Allow"
      actions   = ["secretsmanager:GetSecretValue"]
      resources = [statement.value.arn]
    }
  }

  statement {
    sid       = "ReadDdnsConfig"
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = ["${aws_secretsmanager_secret.ddns.arn}*"]
  }

  # Finding a task's public IP to publish as DNS: the address lives on the
  # task's network interface, so it takes both an ECS and an EC2 lookup.
  statement {
    sid    = "ResolveTaskAddresses"
    effect = "Allow"
    actions = [
      "ecs:ListTasks",
      "ecs:DescribeTasks",
      "ec2:DescribeNetworkInterfaces",
    ]
    resources = ["*"]
  }

  statement {
    sid    = "Logs"
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]
    resources = ["arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:log-group:/codebuild/${local.name}-*"]
  }
}

resource "aws_iam_role_policy" "codebuild" {
  name   = "${local.name}-codebuild"
  role   = aws_iam_role.codebuild.id
  policy = data.aws_iam_policy_document.codebuild.json
}

# ---------------------------------------------------------------------------
# GitHub deployment status. Terraform creates the secret but never its
# value — storing a GitHub token in Terraform would put it in plaintext in
# the state file. Put the token in by hand, once (see infra/README.md);
# until then the reporting script skips quietly and deploys still work.
# ---------------------------------------------------------------------------

resource "aws_secretsmanager_secret" "github_token" {
  count                   = var.enable_github_deployment_status ? 1 : 0
  name                    = "${local.name}/github-deployment-token"
  description             = "GitHub token with `deployments: write`, used to report deploy status back to the repo."
  recovery_window_in_days = 0
  tags                    = local.common_tags
}

# ---------------------------------------------------------------------------
# Dynamic DNS. Without a load balancer the tasks are addressed by IPs that
# change on every deployment; a free DDNS hostname updated after each deploy
# gives back a stable address. Terraform creates the secret, never its value
# — see infra/README.md for the shape.
# ---------------------------------------------------------------------------

resource "aws_secretsmanager_secret" "ddns" {
  name                    = "${local.name}/ddns"
  description             = "Dynamic-DNS provider token and hostnames, as {\"provider\":\"duckdns\",\"token\":\"...\",\"app\":\"...\",\"api\":\"...\"}."
  recovery_window_in_days = 0
  tags                    = local.common_tags
}

# ---------------------------------------------------------------------------
# One CodeBuild project per app, invoked as a Build action inside the
# CodePipeline defined in infra/modules/codepipeline — never started
# directly. Source type CODEPIPELINE means CodePipeline hands the project
# the whole repo checkout on every build; each project's buildspec path
# picks out just its own app's buildspec.yml.
# ---------------------------------------------------------------------------

resource "aws_cloudwatch_log_group" "codebuild" {
  for_each          = var.apps
  name              = "/codebuild/${local.name}-${each.key}"
  retention_in_days = 30
  tags              = local.common_tags
}

resource "aws_codebuild_project" "this" {
  for_each = var.apps

  name         = "${local.name}-${each.key}"
  service_role = aws_iam_role.codebuild.arn
  # Multi-stage Docker builds can run long on the smallest compute size;
  # 20 minutes covers `mvn package` / `npm ci && next build` comfortably.
  build_timeout = 20

  # Must be CODEPIPELINE (matching source.type below) whenever this project
  # is invoked as a CodePipeline Build action — the pipeline requires it
  # even though the buildspec doesn't produce meaningful output files (the
  # deploy happens inline, in post_build).
  artifacts {
    type = "CODEPIPELINE"
  }

  environment {
    compute_type    = "BUILD_GENERAL1_SMALL"
    image           = "aws/codebuild/standard:7.0"
    type            = "LINUX_CONTAINER"
    privileged_mode = true # required for `docker build`

    environment_variable {
      name  = "AWS_REGION"
      value = var.aws_region
    }
    environment_variable {
      name  = "ECR_REGISTRY"
      value = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com"
    }
    environment_variable {
      name = "IMAGE_REPO_NAME"
      # The repository name is the last path segment of the ECR URL.
      value = reverse(split("/", each.value.ecr_repository_url))[0]
    }
    environment_variable {
      name  = "CLUSTER_NAME"
      value = var.ecs_cluster_name
    }
    environment_variable {
      name  = "SERVICE_NAME"
      value = each.value.ecs_service_name
    }
    environment_variable {
      name  = "APP_NAME"
      value = each.key
    }
    environment_variable {
      name  = "ENVIRONMENT_NAME"
      value = var.environment
    }
    environment_variable {
      name  = "APP_BASE_URL"
      value = var.app_base_url
    }
    # Baked into the frontend bundle at image-build time — see
    # frontend/Dockerfile. Changing it needs a rebuild, not a redeploy.
    environment_variable {
      name  = "API_BASE_URL"
      value = var.api_base_url
    }
    environment_variable {
      name  = "GITHUB_REPO"
      value = var.github_repo
    }
    # The ARN, not the token: the buildspec reads the value at run time, so
    # an unconfigured secret degrades to "no status reported" instead of
    # failing every build the way a SECRETS_MANAGER-typed variable would.
    environment_variable {
      name  = "GITHUB_TOKEN_SECRET_ARN"
      value = try(aws_secretsmanager_secret.github_token[0].arn, "")
    }
  }

  source {
    type      = "CODEPIPELINE"
    buildspec = "${each.key}/buildspec.yml"
  }

  logs_config {
    cloudwatch_logs {
      group_name = aws_cloudwatch_log_group.codebuild[each.key].name
    }
  }

  tags = local.common_tags
}

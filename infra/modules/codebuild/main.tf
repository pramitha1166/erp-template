locals {
  name        = "${var.project}-${var.environment}"
  common_tags = merge(var.tags, { ManagedBy = "terraform" })
}

data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------
# Source zips, uploaded by the thin GitHub Actions trigger step and read by
# CodeBuild via a per-build sourceLocationOverride. Short-lived — a build
# consumes its zip within minutes of upload.
# ---------------------------------------------------------------------------

resource "aws_s3_bucket" "source" {
  bucket = "${local.name}-build-source"
  tags   = local.common_tags
}

resource "aws_s3_bucket_public_access_block" "source" {
  bucket                  = aws_s3_bucket.source.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "source" {
  bucket = aws_s3_bucket.source.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "source" {
  bucket = aws_s3_bucket.source.id
  rule {
    id     = "expire-old-source-zips"
    status = "Enabled"
    filter {}
    expiration {
      days = var.source_zip_retention_days
    }
  }
}

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

  statement {
    sid       = "ReadSourceZips"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.source.arn}/*"]
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
# One CodeBuild project per app. `source` is a placeholder — every real
# build overrides it via `start-build --source-location-override` (see
# .github/workflows/deploy.yml), so the value here is never actually built.
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

  artifacts {
    type = "NO_ARTIFACTS"
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
      # A single capture group makes `regex` return the substring directly.
      value = regex("/([^/]+)$", each.value.ecr_repository_url)
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
      # Real value always comes from an environmentVariablesOverride at
      # start-build time (the deploying commit's SHA); this default only
      # matters if someone starts a build from the console without one.
      name  = "IMAGE_TAG"
      value = "manual"
    }
  }

  source {
    type      = "S3"
    location  = "${aws_s3_bucket.source.bucket}/unused-placeholder.zip"
    buildspec = "buildspec.yml"
  }

  logs_config {
    cloudwatch_logs {
      group_name = aws_cloudwatch_log_group.codebuild[each.key].name
    }
  }

  tags = local.common_tags
}

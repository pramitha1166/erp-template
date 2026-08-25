locals {
  name        = "${var.project}-${var.environment}"
  common_tags = merge(var.tags, { ManagedBy = "terraform" })
}

data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------
# Artifact bucket — required by CodePipeline to pass the source checkout
# from the Source stage to each Build action. Versioning is a hard
# CodePipeline requirement, not a preference.
# ---------------------------------------------------------------------------

resource "aws_s3_bucket" "artifacts" {
  bucket = "${local.name}-pipeline-artifacts"
  tags   = local.common_tags
}

resource "aws_s3_bucket_versioning" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket                  = aws_s3_bucket.artifacts.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ---------------------------------------------------------------------------
# GitHub connection. Created in PENDING status — completing it requires a
# one-time manual step in the AWS Console (Developer Tools > Connections >
# "Update pending connection"), which authorizes the AWS Connector for
# GitHub App. Terraform cannot do this part; GitHub's OAuth handshake
# requires a signed-in human in a browser. See infra/README.md.
# ---------------------------------------------------------------------------

resource "aws_codestarconnections_connection" "github" {
  name          = "${local.name}-github"
  provider_type = "GitHub"
  tags          = local.common_tags
}

# ---------------------------------------------------------------------------
# Pipeline execution role.
# ---------------------------------------------------------------------------

data "aws_iam_policy_document" "pipeline_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codepipeline.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "pipeline" {
  name               = "${local.name}-codepipeline"
  assume_role_policy = data.aws_iam_policy_document.pipeline_assume.json
  tags               = local.common_tags
}

data "aws_iam_policy_document" "pipeline" {
  statement {
    sid    = "ArtifactBucket"
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:GetBucketVersioning",
      "s3:ListBucket",
    ]
    resources = [
      aws_s3_bucket.artifacts.arn,
      "${aws_s3_bucket.artifacts.arn}/*",
    ]
  }

  statement {
    sid       = "UseGithubConnection"
    effect    = "Allow"
    actions   = ["codestar-connections:UseConnection"]
    resources = [aws_codestarconnections_connection.github.arn]
  }

  statement {
    sid    = "RunCodeBuildProjects"
    effect = "Allow"
    actions = [
      "codebuild:StartBuild",
      "codebuild:BatchGetBuilds",
    ]
    resources = [for arn in var.codebuild_project_arns : arn]
  }
}

resource "aws_iam_role_policy" "pipeline" {
  name   = "${local.name}-codepipeline"
  role   = aws_iam_role.pipeline.id
  policy = data.aws_iam_policy_document.pipeline.json
}

# CodeBuild reads the source CodePipeline places in the artifact bucket —
# granted here (on the codebuild module's existing service role) rather
# than passed back into that module, so this module depends on codebuild's
# outputs without codebuild needing to know this bucket exists.
data "aws_iam_policy_document" "codebuild_read_artifacts" {
  statement {
    effect    = "Allow"
    actions   = ["s3:GetObject", "s3:GetObjectVersion"]
    resources = ["${aws_s3_bucket.artifacts.arn}/*"]
  }
}

resource "aws_iam_role_policy" "codebuild_read_artifacts" {
  name   = "${local.name}-read-pipeline-artifacts"
  role   = var.codebuild_service_role_name
  policy = data.aws_iam_policy_document.codebuild_read_artifacts.json
}

# ---------------------------------------------------------------------------
# The pipeline: Source (GitHub, via the connection above) -> Build (backend
# and frontend CodeBuild projects, running in parallel — each one builds,
# pushes to ECR, and force-redeploys its own ECS service in its buildspec's
# post_build phase, so no separate Deploy stage is needed).
# ---------------------------------------------------------------------------

resource "aws_codepipeline" "this" {
  name     = local.name
  role_arn = aws_iam_role.pipeline.arn

  artifact_store {
    type     = "S3"
    location = aws_s3_bucket.artifacts.bucket
  }

  stage {
    name = "Source"

    action {
      name             = "Source"
      category         = "Source"
      owner            = "AWS"
      provider         = "CodeStarSourceConnection"
      version          = "1"
      output_artifacts = ["source_output"]

      configuration = {
        ConnectionArn    = aws_codestarconnections_connection.github.arn
        FullRepositoryId = var.github_repo
        BranchName       = var.branch_name
      }
    }
  }

  stage {
    name = "Build"

    dynamic "action" {
      for_each = var.codebuild_project_names
      content {
        name             = title(action.key)
        category         = "Build"
        owner            = "AWS"
        provider         = "CodeBuild"
        version          = "1"
        input_artifacts  = ["source_output"]
        output_artifacts = ["${action.key}_build_output"]
        run_order        = 1 # same run_order across actions in a stage = parallel

        configuration = {
          ProjectName = action.value
        }
      }
    }
  }

  tags = local.common_tags
}

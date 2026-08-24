provider "aws" {
  region = var.aws_region
}

data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------
# Remote state bucket, shared by every environment under infra/environments.
# Native S3 state locking (Terraform >= 1.10) is used, so no DynamoDB lock
# table is needed.
# ---------------------------------------------------------------------------

resource "aws_s3_bucket" "tfstate" {
  bucket = "${var.project}-tfstate-${data.aws_caller_identity.current.account_id}"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "tfstate" {
  bucket                  = aws_s3_bucket.tfstate.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_policy" "tfstate_require_tls" {
  bucket = aws_s3_bucket.tfstate.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "DenyInsecureTransport"
      Effect    = "Deny"
      Principal = "*"
      Action    = "s3:*"
      Resource = [
        aws_s3_bucket.tfstate.arn,
        "${aws_s3_bucket.tfstate.arn}/*",
      ]
      Condition = {
        Bool = { "aws:SecureTransport" = "false" }
      }
    }]
  })
}

# ---------------------------------------------------------------------------
# GitHub Actions OIDC trust — lets workflows in this repo assume AWS IAM
# roles via short-lived tokens instead of long-lived access keys.
#
# The thumbprint is fetched live from GitHub's OIDC issuer rather than
# hardcoded: AWS has not actually validated this value against the
# certificate chain since 2023 for issuers (like GitHub's) whose CA is in
# Amazon's trusted root store, but the API still requires a syntactically
# valid one, and computing it live means this never goes stale if GitHub
# rotates their certificate.
# ---------------------------------------------------------------------------

data "tls_certificate" "github_actions_oidc" {
  url = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  thumbprint_list = [
    data.tls_certificate.github_actions_oidc.certificates[0].sha1_fingerprint,
  ]
}

locals {
  oidc_sub_any_ref  = "repo:${var.github_repo}:*"
  oidc_sub_main_env = "repo:${var.github_repo}:environment:${var.environment_name}"

  role_resource_prefix = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${var.project}-*"
}

data "aws_iam_policy_document" "assume_role_any_ref" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [local.oidc_sub_any_ref]
    }
  }
}

data "aws_iam_policy_document" "assume_role_protected_env" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # Requires the workflow job to declare `environment: staging` (or
    # whatever var.environment_name is) — GitHub only mints a token with
    # this subject after any required reviewers on that Environment approve.
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [local.oidc_sub_main_env]
    }
  }
}

# ---------------------------------------------------------------------------
# Role 1: `terraform plan` on pull requests. Read-only, trusted for any ref
# in the repo — a PR can safely see what a change *would* do.
# ---------------------------------------------------------------------------

resource "aws_iam_role" "terraform_plan" {
  name               = "${var.project}-gha-terraform-plan"
  assume_role_policy = data.aws_iam_policy_document.assume_role_any_ref.json
}

resource "aws_iam_role_policy_attachment" "terraform_plan_readonly" {
  role       = aws_iam_role.terraform_plan.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

# ---------------------------------------------------------------------------
# Role 2: `terraform apply` on merge to main. Gated by the "staging" GitHub
# Environment (configure required reviewers on it in repo settings) since
# this role can create/modify/destroy most of the app's AWS footprint.
# ---------------------------------------------------------------------------

resource "aws_iam_role" "terraform_apply" {
  name               = "${var.project}-gha-terraform-apply"
  assume_role_policy = data.aws_iam_policy_document.assume_role_protected_env.json
}

data "aws_iam_policy_document" "terraform_apply" {
  statement {
    sid    = "Infrastructure"
    effect = "Allow"
    actions = [
      "ec2:*",
      "ecs:*",
      "ecr:*",
      "rds:*",
      "elasticache:*",
      "elasticloadbalancing:*",
      "logs:*",
      "secretsmanager:*",
      "application-autoscaling:*",
      "s3:*",
      "servicediscovery:*",
    ]
    resources = ["*"]
  }

  statement {
    # IAM is the dangerous one: scoped to roles/policies this project
    # creates (name prefix), never to arbitrary IAM entities.
    sid    = "ScopedIam"
    effect = "Allow"
    actions = [
      "iam:CreateRole",
      "iam:DeleteRole",
      "iam:GetRole",
      "iam:TagRole",
      "iam:UntagRole",
      "iam:UpdateRole",
      "iam:UpdateAssumeRolePolicy",
      "iam:PutRolePolicy",
      "iam:DeleteRolePolicy",
      "iam:GetRolePolicy",
      "iam:AttachRolePolicy",
      "iam:DetachRolePolicy",
      "iam:ListRolePolicies",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
      "iam:PassRole",
      "iam:CreateServiceLinkedRole",
    ]
    resources = [local.role_resource_prefix]
  }

  statement {
    sid       = "TagValidation"
    effect    = "Allow"
    actions   = ["iam:ListRoles"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "terraform_apply" {
  name   = "${var.project}-terraform-apply"
  role   = aws_iam_role.terraform_apply.id
  policy = data.aws_iam_policy_document.terraform_apply.json
}

# ---------------------------------------------------------------------------
# Role 3: upload a source zip and trigger the AWS-side build. Deliberately
# minimal — no ECR, ECS, IAM, VPC, or database permissions at all. The
# actual image build/push/deploy runs inside AWS CodeBuild
# (infra/modules/codebuild), under its own service role, so GitHub Actions
# never needs those permissions and barely spends any of its own compute
# minutes on a deploy — see .github/workflows/deploy.yml.
# ---------------------------------------------------------------------------

resource "aws_iam_role" "app_deploy" {
  name               = "${var.project}-gha-app-deploy"
  assume_role_policy = data.aws_iam_policy_document.assume_role_protected_env.json
}

data "aws_iam_policy_document" "app_deploy" {
  statement {
    sid       = "UploadBuildSource"
    effect    = "Allow"
    actions   = ["s3:PutObject"]
    resources = ["arn:aws:s3:::${var.project}-*-build-source/*"]
  }

  statement {
    sid    = "TriggerAndPollCodeBuild"
    effect = "Allow"
    actions = [
      "codebuild:StartBuild",
      "codebuild:BatchGetBuilds",
    ]
    resources = ["arn:aws:codebuild:${var.aws_region}:${data.aws_caller_identity.current.account_id}:project/${var.project}-*"]
  }
}

resource "aws_iam_role_policy" "app_deploy" {
  name   = "${var.project}-app-deploy"
  role   = aws_iam_role.app_deploy.id
  policy = data.aws_iam_policy_document.app_deploy.json
}

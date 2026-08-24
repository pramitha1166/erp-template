data "aws_caller_identity" "current" {}

locals {
  bucket_name = "${var.project}-${var.environment}-attachments-${data.aws_caller_identity.current.account_id}"
  common_tags = merge(var.tags, { ManagedBy = "terraform" })
}

# DOC-1: file attachment storage. S3-compatible, mirrors the MinIO bucket
# used in docker-compose for local dev.
resource "aws_s3_bucket" "attachments" {
  bucket = local.bucket_name
  tags   = local.common_tags
}

resource "aws_s3_bucket_versioning" "attachments" {
  bucket = aws_s3_bucket.attachments.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "attachments" {
  bucket = aws_s3_bucket.attachments.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "attachments" {
  bucket                  = aws_s3_bucket.attachments.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    # Filled in via `terraform init -backend-config=backend.hcl`.
    # Copy backend.hcl.example to backend.hcl (gitignored) and set the
    # bucket name from the bootstrap stack's `state_bucket` output.
    # See infra/README.md.
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

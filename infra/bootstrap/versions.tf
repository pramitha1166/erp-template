terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Deliberately local state: this stack creates the S3 bucket that every
  # other stack's remote state lives in, so it can't depend on that bucket
  # existing yet. Run this once, by hand, from a machine with your own AWS
  # credentials (see infra/README.md). The resulting terraform.tfstate file
  # is sensitive (it contains no secrets itself, but is the source of truth
  # for who can assume the CI roles) — keep it out of git and store it
  # somewhere durable (e.g. copy it into the state bucket it just created,
  # or a password manager / secure note).
}

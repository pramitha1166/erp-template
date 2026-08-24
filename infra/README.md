# Infrastructure — AWS (staging)

Terraform-managed AWS infrastructure for the `staging` environment, plus
the GitHub Actions pipelines that deploy to it. See root `CLAUDE.md` for
the app-level (`backend/`, `frontend/`) commands; this covers the
infrastructure and CI/CD layer.

## Architecture

```
Internet
   │
   ▼
Application Load Balancer (public subnets)
   │  "/"      → frontend target group
   │  "/api/*" → backend target group
   ▼
ECS Fargate (private subnets)
   ├── frontend service (Next.js, :3000)
   └── backend service  (Spring Boot, :8080, context-path /api)
        │
        ├── RDS Postgres 16 (private subnet, RDS SG only)
        ├── ElastiCache Redis (private subnet, RDS SG only)
        └── S3 (attachments — DOC-1)

ECR: one repository per app image, pulled by the ECS execution role.
Secrets Manager: DB credentials, injected into the task definition via
`secrets` (never a plaintext environment variable).
```

Single NAT gateway, single-AZ RDS/Redis — cost-optimized for a staging
environment, not production. See "Scaling up" below for what to flip when
this needs to become a real production tier.

## Module layout

```
infra/
  bootstrap/            One-time, human-run, local state. Creates the
                         Terraform state bucket and the three GitHub OIDC
                         IAM roles CI assumes. Nothing else depends on this
                         being re-applied often.
  modules/
    network/             VPC, public+private subnets, IGW, NAT
    ecr/                 Container image repositories
    storage/             S3 attachments bucket
    rds/                 Postgres + Secrets Manager credential
    redis/                ElastiCache
    alb/                  Load balancer, target groups, listener(s)
    ecs/                  Cluster, task definitions, services, IAM
  environments/
    staging/              Wires every module together for one environment
```

## One-time setup (do this once, by hand)

You need an AWS account with console/CLI access and permission to create
IAM roles, an OIDC provider, and an S3 bucket.

### 1. Bootstrap the state backend and CI roles

```bash
cd infra/bootstrap
terraform init
terraform apply
```

Review the plan — it creates:
- An S3 bucket for Terraform remote state (versioned, encrypted, TLS-only).
- A GitHub Actions OIDC provider trusting `token.actions.githubusercontent.com`.
- Three IAM roles CI assumes via OIDC (no AWS access keys anywhere):
  - `eudext-erp-gha-terraform-plan` — read-only, any branch/PR.
  - `eudext-erp-gha-terraform-apply` — can create/modify/destroy the app's
    AWS footprint. Trusted only for the `staging` GitHub Environment.
  - `eudext-erp-gha-app-deploy` — ECR push + `ecs:UpdateService` only.
    Trusted only for the `staging` GitHub Environment.

Keep `infra/bootstrap/terraform.tfstate` somewhere durable (it's not
secret, but it's the only record of what this stack created) — it is not,
and should not be, committed to git.

Note the four outputs — you'll need them next:

```bash
terraform output
```

### 2. Configure the "staging" GitHub Environment

In the repo's Settings → Environments, create an environment named
`staging`. Add required reviewers if you want a human approval gate before
`terraform apply` and before every deploy (recommended). This is what the
IAM trust policy above keys off — without this environment configured,
the `terraform-apply` and `app-deploy` roles cannot be assumed at all.

### 3. Set repository variables

Settings → Secrets and variables → Actions → Variables (not secrets — none
of these are sensitive, they're ARNs and a region):

| Variable | Value |
|---|---|
| `AWS_REGION` | `ap-south-1` (or whatever you set `aws_region` to) |
| `TF_STATE_BUCKET` | `state_bucket` output from step 1 |
| `TF_PLAN_ROLE_ARN` | `terraform_plan_role_arn` output |
| `TF_APPLY_ROLE_ARN` | `terraform_apply_role_arn` output |
| `APP_DEPLOY_ROLE_ARN` | `app_deploy_role_arn` output |

### 4. First `terraform apply`

Either open a PR touching `infra/` (runs `plan` for review) and merge it —
`.github/workflows/terraform.yml`'s `apply` job runs on merge to `main` —
or run it yourself once from a machine with your AWS credentials:

```bash
cd infra/environments/staging
cp backend.hcl.example backend.hcl   # fill in the real bucket name
terraform init -backend-config=backend.hcl
terraform plan
terraform apply
```

This creates the VPC, RDS, Redis, ALB, ECS cluster/services, and empty ECR
repositories. **The ECS services will sit in a pending/failing state until
the first image is pushed** — that's expected, not a bug. Continue to step 5.

### 5. First deploy

`.github/workflows/deploy.yml` triggers automatically after `CI` succeeds
on `main` (via `workflow_run`), so the very next push to `main` after step
4 will build both images, push `:staging` and `:<sha>` tags to ECR, and
force a new ECS deployment. After that, `terraform apply` (infra changes)
and `deploy` (app changes) run independently — a normal code change never
touches Terraform, and an infra change never rebuilds the app images.

Find the app at the `alb_dns_name` output (plain HTTP until a certificate
is configured — see below).

## Day 2

**Adding a custom domain + HTTPS (BRD-4):** request/import a certificate
in ACM for the domain, point a Route 53 (or your DNS provider's) record at
the ALB, then set the `certificate_arn` variable in
`infra/environments/staging/variables.tf` (or pass
`-var certificate_arn=...`) and re-apply. The ALB module adds an HTTPS
listener + HTTP→HTTPS redirect automatically once that variable is set.

**Changing instance sizes:** `rds_instance_class`, `redis_node_type`,
`backend_cpu`/`backend_memory`, `frontend_cpu`/`frontend_memory` are all
Terraform variables — change and re-apply, no manual console work.

**Rolling back a bad deploy:** every image is also pushed tagged with its
git SHA. Re-point `:staging` at a known-good SHA with:
```bash
aws ecr batch-get-image --repository-name eudext-erp-backend --image-ids imageTag=<good-sha> --query 'images[0].imageManifest' --output text | \
  aws ecr put-image --repository-name eudext-erp-backend --image-tag staging --image-manifest -
aws ecs update-service --cluster eudext-erp-staging --service eudext-erp-staging-backend --force-new-deployment
```

**Scaling up toward production:** flip `multi_az = true` on the RDS
module, move to one NAT gateway per AZ (`single_nat_gateway = false` on
the network module), add a second environment under
`infra/environments/` (e.g. `production/`) with its own state key and its
own `production` GitHub Environment/IAM role trust, and consider Multi-AZ
ElastiCache with automatic failover. None of this is wired up yet — it
was explicitly out of scope for the first cut (see the conversation that
produced this directory).

**Tearing the environment down:**
```bash
cd infra/environments/staging
terraform destroy
```
The state bucket and CI roles from `infra/bootstrap` are separate and
survive this — destroy that stack only if you're decommissioning entirely.

## Security notes

- No AWS access keys exist anywhere in this repo or in GitHub secrets —
  every workflow authenticates via short-lived OIDC-federated STS
  credentials, scoped by role and (for apply/deploy) gated behind the
  `staging` Environment's required reviewers.
- `terraform-apply`'s IAM permissions are broad within this project's
  services but its own IAM actions are restricted to role/policy names
  prefixed `eudext-erp-*` — it cannot modify unrelated IAM entities in the
  account.
- DB credentials live only in Secrets Manager; the ECS task definition
  references them by ARN, never as plaintext.
- The Terraform state bucket is private, versioned, encrypted, and
  TLS-only. Terraform state contains the DB password in plain text (this
  is normal for the S3 backend) — treat read access to that bucket as
  equivalent to database access.

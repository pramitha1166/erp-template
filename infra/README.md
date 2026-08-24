# Infrastructure — AWS (staging)

Terraform-managed AWS infrastructure for the `staging` environment,
including the AWS CodePipeline that builds and deploys it. See root
`CLAUDE.md` for the app-level (`backend/`, `frontend/`) commands; this
covers the infrastructure and CI/CD layer.

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

### Build & deploy pipeline — AWS CodePipeline, not GitHub Actions

```
GitHub (push to claude/srs-review-breakdown-49ecvy)
   │  via a CodeStar (GitHub App) connection — no webhook secrets to manage
   ▼
AWS CodePipeline
   ├─ Source stage:  pulls the repo checkout
   └─ Build stage:   two CodeBuild actions, running in parallel
        ├─ backend  project → docker build → push :sha + :staging to ECR
        │                   → ecs update-service --force-new-deployment
        │                   → ecs wait services-stable
        └─ frontend project → (same, for the frontend image/service)
```

GitHub Actions is **not involved in build or deploy at all** — CodePipeline
watches the repo directly. GitHub Actions (`.github/workflows/ci.yml`)
still runs backend/frontend tests and lint as PR status checks, since that
gates code review and CodePipeline has no equivalent hook into PRs; it has
nothing to do with what gets deployed.

`.github/workflows/terraform.yml` is unrelated to this pipeline — it's how
*infrastructure* changes (this Terraform code, including the pipeline
itself) get planned and applied, as opposed to *application* code changes,
which CodePipeline handles independently once the infrastructure exists.

## Module layout

```
infra/
  bootstrap/            One-time, human-run, local state. Creates the
                         Terraform state bucket and the two GitHub OIDC IAM
                         roles used only for *infrastructure* changes
                         (terraform-plan, terraform-apply). Nothing here is
                         involved in application deploys.
  modules/
    network/             VPC, public+private subnets, IGW, NAT
    ecr/                 Container image repositories
    storage/             S3 attachments bucket
    rds/                 Postgres + Secrets Manager credential
    redis/                ElastiCache
    alb/                  Load balancer, target groups, listener(s)
    ecs/                  Cluster, task definitions, services, IAM
    codebuild/             Build projects — docker build/push/deploy logic,
                           invoked only as CodePipeline Build actions
    codepipeline/           The pipeline itself: GitHub connection,
                           artifact bucket, pipeline IAM role, stages
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
- Two IAM roles, used only for *Terraform* changes to this infrastructure
  (never for application builds/deploys — CodePipeline handles those under
  its own roles, created directly by Terraform, not via OIDC):
  - `eudext-erp-gha-terraform-plan` — read-only, any branch/PR.
  - `eudext-erp-gha-terraform-apply` — can create/modify/destroy the app's
    AWS footprint, including the CodePipeline/CodeBuild resources
    themselves. Trusted only for the `staging` GitHub Environment.

> Upgrading from an earlier version of this setup that had a third
> `gha-app-deploy` role and a `deploy.yml` workflow? Both are gone —
> deploys now run entirely through CodePipeline. Re-run `terraform apply`
> here to drop the now-unused role, and delete `APP_DEPLOY_ROLE_ARN` from
> your repo variables (step 3) if you'd set it.

Keep `infra/bootstrap/terraform.tfstate` somewhere durable (it's not
secret, but it's the only record of what this stack created) — it is not,
and should not be, committed to git.

Note the outputs — you'll need them next:

```bash
terraform output
```

### 2. Configure the "staging" GitHub Environment

In the repo's Settings → Environments, create an environment named
`staging`. Add required reviewers if you want a human approval gate before
`terraform apply` (recommended). This is what the `terraform-apply` role's
trust policy keys off — without this environment configured, that role
cannot be assumed at all.

### 3. Set repository variables

Settings → Secrets and variables → Actions → Variables (not secrets — none
of these are sensitive, they're ARNs and a region):

| Variable | Value |
|---|---|
| `AWS_REGION` | `ap-south-1` (or whatever you set `aws_region` to) |
| `TF_STATE_BUCKET` | `state_bucket` output from step 1 |
| `TF_PLAN_ROLE_ARN` | `terraform_plan_role_arn` output |
| `TF_APPLY_ROLE_ARN` | `terraform_apply_role_arn` output |

### 4. First `terraform apply`

Either open a PR touching `infra/` (runs `plan` for review) and merge it —
`.github/workflows/terraform.yml`'s `apply` job runs on merge to
`claude/srs-review-breakdown-49ecvy` (the repo's integration branch — there
is currently no `main`) — or run it yourself once from a machine with your
AWS credentials:

```bash
cd infra/environments/staging
cp backend.hcl.example backend.hcl   # fill in the real bucket name
terraform init -backend-config=backend.hcl
terraform plan
terraform apply
```

This creates the VPC, RDS, Redis, ALB, ECS cluster/services, CodeBuild
projects, the CodePipeline itself, and empty ECR repositories. **The ECS
services will sit in a pending/failing state, and the GitHub connection
will be unusable, until step 5** — that's expected, not a bug.

### 5. Authorize the GitHub connection (the one unavoidable manual step)

`terraform apply` creates the CodeStar GitHub connection in `PENDING`
status — completing it requires a signed-in human clicking through GitHub's
OAuth screen, which cannot be scripted:

1. AWS Console → **Developer Tools → Settings → Connections**.
2. Find the connection named `eudext-erp-staging-github` → status `Pending`.
3. Click it → **Update pending connection**.
4. Choose **Install a new app** (first time) or your existing "AWS
   Connector for GitHub" installation, authorize it for
   `pramitha1166/erp-template` (or all repos, your choice), and complete
   the connection.
5. Status flips to `Available`. From this point on, every push to
   `claude/srs-review-breakdown-49ecvy` automatically triggers the
   pipeline — no further action needed.

Verify from the CLI instead of the console if you prefer:
```bash
aws codestar-connections get-connection \
  --connection-arn "$(cd infra/environments/staging && terraform output -raw codepipeline_connection_arn)" \
  --query 'Connection.ConnectionStatus'
```

### 6. First deploy

Once the connection is `Available`, push to
`claude/srs-review-breakdown-49ecvy` (or just re-run the pipeline manually
— AWS Console → CodePipeline → your pipeline → **Release change**) to
trigger the first build. It builds both images, pushes `:staging` and
`:<commit-sha>` tags to ECR, and force-redeploys both ECS services. Track
progress in the CodePipeline console, or in CloudWatch under
`/codebuild/eudext-erp-staging-backend` / `-frontend`.

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

**Re-running a deploy without a new commit:** AWS Console → CodePipeline →
`eudext-erp-staging` → **Release change**. Or via CLI:
```bash
aws codepipeline start-pipeline-execution --name eudext-erp-staging
```

**Scaling up toward production:** flip `multi_az = true` on the RDS
module, move to one NAT gateway per AZ (`single_nat_gateway = false` on
the network module), add a second environment under
`infra/environments/` (e.g. `production/`) with its own state key, its own
`production` GitHub Environment/IAM role trust, and its own CodePipeline
(pointed at a `production` branch or tag pattern), and consider Multi-AZ
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

- No AWS access keys exist anywhere in this repo or in GitHub secrets.
  Infrastructure changes (Terraform) authenticate via short-lived
  OIDC-federated STS credentials, gated behind the `staging` Environment's
  required reviewers for `apply`. Application deploys don't touch GitHub
  credentials at all — CodePipeline authenticates to GitHub via the
  CodeStar connection (a GitHub App installation, not a token you manage),
  and to AWS services under its own IAM role.
- `terraform-apply`'s IAM permissions are broad within this project's
  services but its own IAM actions are restricted to role/policy names
  prefixed `eudext-erp-*` — it cannot modify unrelated IAM entities in the
  account.
- The CodePipeline and CodeBuild IAM roles are scoped separately: the
  pipeline role can only start/poll the two named CodeBuild projects and
  read/write its own artifact bucket; the CodeBuild service role can only
  push to this project's two ECR repos, update the two named ECS services,
  and write to its own log groups.
- DB credentials live only in Secrets Manager; the ECS task definition
  references them by ARN, never as plaintext.
- The Terraform state bucket is private, versioned, encrypted, and
  TLS-only. Terraform state contains the DB password in plain text (this
  is normal for the S3 backend) — treat read access to that bucket as
  equivalent to database access.

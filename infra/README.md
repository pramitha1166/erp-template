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
             each ends by reporting success/failure to GitHub Deployments
```

There are no GitHub Actions workflows in this repo at all — CodePipeline
watches GitHub directly, and tests are run locally before pushing (see the
root `CLAUDE.md`). What GitHub does get back is the *result*: each
CodeBuild project posts to the repo's Deployments API when it finishes, so
a deploy appears on the commit and under the repo's **Environments** tab,
linking to both the running app and the build log.

*Infrastructure* changes (this Terraform code, including the pipeline
itself) are applied by hand, from a workstation with AWS credentials — see
"Applying infrastructure changes" below. There is deliberately no CI
workflow for Terraform. Application code changes are separate, and
CodePipeline handles those on its own once the infrastructure exists.

## Module layout

```
infra/
  bootstrap/            One-time, human-run, local state. Creates the
                         Terraform state bucket. It also defines a GitHub
                         OIDC provider and two IAM roles (terraform-plan,
                         terraform-apply) that nothing uses now that
                         Terraform runs by hand — kept only for re-adding
                         CI-driven Terraform later. Nothing here is
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

### 1. Bootstrap the state backend

```bash
cd infra/bootstrap
terraform init
terraform apply
```

Review the plan — it creates:
- An S3 bucket for Terraform remote state (versioned, encrypted, TLS-only).
- A GitHub Actions OIDC provider and two IAM roles
  (`eudext-erp-gha-terraform-plan`, `eudext-erp-gha-terraform-apply`).
  **Nothing assumes these today** — Terraform is run by hand, and
  CodePipeline builds/deploys under its own roles created directly by
  Terraform, not via OIDC. They are kept so CI-driven Terraform can be
  re-added without redoing the trust-policy work; delete them from
  `bootstrap/main.tf` and re-apply if you would rather not carry an unused
  OIDC trust into the account.

> Upgrading from an earlier version of this setup that had a third
> `gha-app-deploy` role and a `deploy.yml` workflow, or a `terraform.yml`
> workflow? All gone — deploys run entirely through CodePipeline, and
> Terraform runs by hand. Re-run `terraform apply` here to drop the unused
> role, and delete the `APP_DEPLOY_ROLE_ARN`, `TF_PLAN_ROLE_ARN`,
> `TF_APPLY_ROLE_ARN`, and `TF_STATE_BUCKET` repository variables if you
> had set them.

Keep `infra/bootstrap/terraform.tfstate` somewhere durable (it's not
secret, but it's the only record of what this stack created) — it is not,
and should not be, committed to git.

Note the outputs — you'll need them next:

```bash
terraform output
```

### 2. First `terraform apply`

Run it from a machine with your AWS credentials:

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
will be unusable, until step 4** — that's expected, not a bug.

### 3. Store a GitHub deployment token (optional)

Deploys report their outcome back to GitHub, which needs a token. Create a
fine-grained personal access token scoped to this repository with
**Read and write** access to *Deployments*, then store it in the secret
Terraform created for it:

```bash
aws secretsmanager put-secret-value \
  --secret-id "$(terraform -chdir=environments/staging output -raw github_deployment_token_secret_arn)" \
  --secret-string "ghp_your_token_here"
```

Terraform creates the secret but never its value — a token in Terraform
would sit in plaintext in the state file. Until you store one, every
deploy still works and `infra/scripts/github-deployment-status.sh` simply
logs that it is skipping. Set `enable_github_deployment_status = false` on
the codebuild module if you don't want the reporting at all.

### 4. Authorize the GitHub connection (the one unavoidable manual step)

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

### 5. First deploy

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

**Turning the environment off between sessions:** this environment costs
roughly USD 85/month running around the clock, on an account whose Free
plan is credit-based — idle time burns real credits. Use:

```bash
./infra/scripts/env.sh down     # scale to zero, stop the database
./infra/scripts/env.sh up       # back up in ~3 minutes
./infra/scripts/env.sh status
```

`down` takes it to about USD 30/month and keeps the ALB's DNS name, the
database volume, and every configured value, so `up` needs no
reconfiguration. What it cannot switch off is the ALB (~USD 17/month) and
the two Elastic IPs attached to it — neither an ALB nor an ElastiCache node
can be stopped, only destroyed. For a break of more than a week or two,
`terraform destroy` in `environments/staging` takes it to near zero; the
cost is a new ALB DNS name and an empty database on the way back up.

AWS restarts a stopped RDS instance by itself after 7 days. Re-run `down`
if the environment is still idle then.

**Changing a task definition needs an extra step.** Both ECS services
declare `ignore_changes = [task_definition]` so the deploy pipeline owns
what image is running. The side effect: `terraform apply` creates a new
task-definition revision, but the service stays pinned to the old one, and
`--force-new-deployment` re-pulls that same old revision. After changing
container environment variables, secrets, CPU, or memory, move the service
across explicitly:

```bash
aws ecs update-service --cluster eudext-erp-staging \
  --service eudext-erp-staging-backend \
  --task-definition eudext-erp-staging-backend   # resolves to the latest revision
```

**Applying infrastructure changes:** there is no CI for Terraform — edit
the code, then plan and apply it yourself:

```bash
cd infra/environments/staging
terraform plan      # always read this before applying
terraform apply
```

State lives in the shared S3 bucket with `use_lockfile=true`, so a
concurrent apply from somewhere else is blocked rather than racing you. Do
commit the code you applied: the bucket records what the account looks
like, not why, and with no CI plan on PRs the diff in git is the only
review this infrastructure gets.

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
`production` state key and its own CodePipeline
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

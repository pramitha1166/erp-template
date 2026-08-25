#!/usr/bin/env bash
#
# Reports the outcome of a CodeBuild deploy back to GitHub, using the
# Deployments API — the commit gets a deployment record, and the repo's
# Environments page shows what is live in each environment.
#
# Invoked from the `finally` block of a buildspec's post_build phase, so it
# runs whether the deploy succeeded or failed. It never fails the build: a
# broken or unconfigured status report must not mask a successful deploy,
# nor turn a failed one into something more confusing.
#
# Requires GITHUB_TOKEN_SECRET_ARN to point at a Secrets Manager secret
# holding a GitHub token with `deployments: write` on the repo. Without it,
# the script exits quietly — status reporting is optional.

set -uo pipefail

warn() { echo "[github-status] $*" >&2; }

if [ -z "${GITHUB_TOKEN_SECRET_ARN:-}" ] || [ -z "${GITHUB_REPO:-}" ]; then
  warn "GITHUB_TOKEN_SECRET_ARN or GITHUB_REPO unset — skipping."
  exit 0
fi

token=$(aws secretsmanager get-secret-value \
  --secret-id "$GITHUB_TOKEN_SECRET_ARN" \
  --query SecretString --output text 2>/dev/null)

# An empty secret is the expected state until someone stores a token in it.
if [ -z "$token" ] || [ "$token" = "None" ]; then
  warn "no token in $GITHUB_TOKEN_SECRET_ARN — skipping. See infra/README.md."
  exit 0
fi

# CodeBuild sets this to 1 while the build is still passing; the `finally`
# block is the only place it reflects the outcome of post_build itself.
if [ "${CODEBUILD_BUILD_SUCCEEDING:-0}" = "1" ]; then
  state="success"
  description="Deployed ${APP_NAME:-app} to ${ENVIRONMENT_NAME:-staging}"
else
  state="failure"
  description="Deploy of ${APP_NAME:-app} to ${ENVIRONMENT_NAME:-staging} failed"
fi

api() {
  curl -sS --fail-with-body -X POST \
    -H "Authorization: Bearer $token" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "$@"
}

# One deployment record per app per build, so backend and frontend report
# independently rather than one overwriting the other's status.
deployment_id=$(api \
  "https://api.github.com/repos/${GITHUB_REPO}/deployments" \
  -d "$(jq -nc \
    --arg ref "$CODEBUILD_RESOLVED_SOURCE_VERSION" \
    --arg env "${ENVIRONMENT_NAME:-staging}-${APP_NAME:-app}" \
    --arg desc "$description" \
    '{ref: $ref, environment: $env, description: $desc,
      auto_merge: false, required_contexts: [], transient_environment: false}')" \
  | jq -r '.id // empty')

if [ -z "$deployment_id" ]; then
  warn "could not create a deployment record — check the token's permissions."
  exit 0
fi

log_url="https://${AWS_REGION}.console.aws.amazon.com/codesuite/codebuild/projects/${CODEBUILD_BUILD_ID%%:*}/build/${CODEBUILD_BUILD_ID}?region=${AWS_REGION}"

api \
  "https://api.github.com/repos/${GITHUB_REPO}/deployments/${deployment_id}/statuses" \
  -d "$(jq -nc \
    --arg state "$state" \
    --arg desc "$description" \
    --arg log "$log_url" \
    --arg url "${APP_BASE_URL:-}" \
    '{state: $state, description: $desc, log_url: $log}
     + (if $url == "" then {} else {environment_url: $url} end)')" \
  >/dev/null || warn "deployment created but status post failed."

echo "[github-status] reported $state for ${APP_NAME:-app} (deployment $deployment_id)."

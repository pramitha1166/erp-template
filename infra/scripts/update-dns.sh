#!/usr/bin/env bash
#
# Points a free dynamic-DNS hostname at whatever IP the ECS tasks currently
# have, so the environment keeps a stable address without a load balancer.
#
# Fargate tasks get a new public IP every time they start — on a deploy, on
# `env.sh up`, or whenever ECS replaces a task. Run after either of those and
# the hostnames follow. Called automatically from env.sh and from the deploy
# buildspec; safe to run by hand at any time.
#
# Configuration lives in one Secrets Manager secret ("<cluster>/ddns"), so
# changing a hostname needs no redeploy:
#
#   {"provider":"duckdns","token":"...","app":"my-erp","api":"my-erp-api"}
#
# Without that secret the script exits quietly — the hostnames are optional,
# and a deploy must not fail because DNS bookkeeping did not work out.

set -uo pipefail

REGION="${AWS_REGION:-ap-south-1}"
CLUSTER="${CLUSTER_NAME:-eudext-erp-staging}"
SECRET_ID="${DDNS_SECRET_ID:-${CLUSTER}/ddns}"

warn() { echo "[ddns] $*" >&2; }
aws() { command aws --region "$REGION" --no-cli-pager "$@"; }

config=$(aws secretsmanager get-secret-value --secret-id "$SECRET_ID" \
  --query SecretString --output text 2>/dev/null)

if [ -z "$config" ] || [ "$config" = "None" ]; then
  warn "no configuration in $SECRET_ID — skipping. See infra/README.md."
  exit 0
fi

token=$(printf '%s' "$config" | jq -r '.token // empty')
if [ -z "$token" ]; then
  warn "no token in $SECRET_ID — skipping."
  exit 0
fi

# The public IP is on the task's network interface, not on the task itself.
task_ip() {
  local task eni
  task=$(aws ecs list-tasks --cluster "$CLUSTER" --service-name "$1" \
    --desired-status RUNNING --query 'taskArns[0]' --output text 2>/dev/null)
  [ -z "$task" ] || [ "$task" = "None" ] && return 1

  eni=$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$task" \
    --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value | [0]" \
    --output text 2>/dev/null)
  [ -z "$eni" ] || [ "$eni" = "None" ] && return 1

  local ip
  ip=$(aws ec2 describe-network-interfaces --network-interface-ids "$eni" \
    --query 'NetworkInterfaces[0].Association.PublicIp' --output text 2>/dev/null)
  [ -z "$ip" ] || [ "$ip" = "None" ] && return 1
  printf '%s' "$ip"
}

update() {
  local host="$1" ip="$2" result
  # DuckDNS answers "OK"/"KO" in the body with 200 either way, so the body is
  # the only thing that says whether the record actually moved.
  result=$(curl -sS --max-time 20 \
    "https://www.duckdns.org/update?domains=${host}&token=${token}&ip=${ip}" || echo "KO")
  if [ "$result" = "OK" ]; then
    echo "[ddns] ${host}.duckdns.org -> ${ip}"
  else
    warn "${host}.duckdns.org update refused (check the token and that the domain exists)"
  fi
}

status=0
for entry in "app:${CLUSTER}-frontend" "api:${CLUSTER}-backend"; do
  key="${entry%%:*}"
  service="${entry#*:}"

  host=$(printf '%s' "$config" | jq -r --arg k "$key" '.[$k] // empty')
  [ -z "$host" ] && continue

  if ! ip=$(task_ip "$service"); then
    warn "no running task for $service — leaving ${host}.duckdns.org alone"
    continue
  fi
  update "$host" "$ip"
done

exit $status

#!/usr/bin/env bash
#
# Start and stop the staging environment, so it only costs money while
# someone is actually working on it.
#
#   ./infra/scripts/env.sh down     # ~$85/mo  ->  ~$30/mo
#   ./infra/scripts/env.sh up       # back in ~3 minutes
#   ./infra/scripts/env.sh status
#
# `down` scales both ECS services to zero and stops the RDS instance. It
# keeps the ALB, its DNS name, and the database volume, so `up` is fast and
# nothing has to be reconfigured. What it cannot switch off is the ALB and
# its Elastic IPs — a load balancer cannot be stopped, only destroyed — so a
# long break is better served by `terraform destroy` (see infra/README.md).
#
# Note: AWS restarts a stopped RDS instance automatically after 7 days.
# Run `down` again after that if the environment is still idle.

set -euo pipefail

REGION="${AWS_REGION:-ap-south-1}"
CLUSTER="${CLUSTER:-eudext-erp-staging}"
DB="${DB:-eudext-erp-staging}"
BACKEND="${BACKEND:-eudext-erp-staging-backend}"
FRONTEND="${FRONTEND:-eudext-erp-staging-frontend}"
BACKEND_COUNT="${BACKEND_COUNT:-1}"
FRONTEND_COUNT="${FRONTEND_COUNT:-1}"

aws() { command aws --region "$REGION" --no-cli-pager "$@"; }

scale() {
  aws ecs update-service --cluster "$CLUSTER" --service "$1" --desired-count "$2" \
    --query 'service.serviceName' --output text >/dev/null
  echo "  $1 -> desired $2"
}

db_status() {
  aws rds describe-db-instances --db-instance-identifier "$DB" \
    --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "absent"
}

case "${1:-status}" in
  down)
    echo "Scaling services to zero..."
    scale "$BACKEND" 0
    scale "$FRONTEND" 0

    status=$(db_status)
    if [ "$status" = "available" ]; then
      echo "Stopping the database..."
      aws rds stop-db-instance --db-instance-identifier "$DB" \
        --query 'DBInstance.DBInstanceStatus' --output text
    else
      echo "  database is '$status' — leaving it alone"
    fi
    echo
    echo "Down. Roughly \$30/month keeps running — the ALB and its two"
    echo "Elastic IPs are most of it. See infra/README.md to destroy those too."
    ;;

  up)
    status=$(db_status)
    if [ "$status" = "stopped" ]; then
      echo "Starting the database (takes a few minutes)..."
      aws rds start-db-instance --db-instance-identifier "$DB" \
        --query 'DBInstance.DBInstanceStatus' --output text
    else
      echo "Database is '$status'."
    fi

    # The backend fails its health check without a reachable database, and
    # ECS would then kill the tasks — so wait for RDS before scaling up.
    echo "Waiting for the database to accept connections..."
    aws rds wait db-instance-available --db-instance-identifier "$DB"

    echo "Scaling services back up..."
    scale "$BACKEND" "$BACKEND_COUNT"
    scale "$FRONTEND" "$FRONTEND_COUNT"

    echo "Waiting for tasks to go healthy (the backend takes ~1 minute to boot)..."
    aws ecs wait services-stable --cluster "$CLUSTER" --services "$BACKEND" "$FRONTEND"

    url=$(aws elbv2 describe-load-balancers --names "$CLUSTER" \
      --query 'LoadBalancers[0].DNSName' --output text 2>/dev/null || true)
    echo
    echo "Up. http://${url}"
    ;;

  status)
    echo "Database: $(db_status)"
    aws ecs describe-services --cluster "$CLUSTER" --services "$BACKEND" "$FRONTEND" \
      --query 'services[].{service:serviceName,desired:desiredCount,running:runningCount}' \
      --output table
    ;;

  *)
    echo "usage: $0 {up|down|status}" >&2
    exit 2
    ;;
esac

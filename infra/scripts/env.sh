#!/usr/bin/env bash
#
# Start and stop the staging environment, so it only costs money while
# someone is actually working on it.
#
#   ./infra/scripts/env.sh down     # ~$60/mo  ->  ~$5/mo
#   ./infra/scripts/env.sh up       # back in ~3 minutes
#   ./infra/scripts/env.sh status
#
# `down` scales both ECS services to zero and stops the RDS instance; `up`
# reverses it and prints the URLs. There is no load balancer in this
# environment, so the tasks are reached on their own public IPs — which are
# new every time they start. Always take the addresses from `up`/`status`;
# never bookmark them.
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

# The public IP lives on the task's ENI, not on the task, so it takes a
# second hop through EC2 to find it.
task_ip() {
  local task
  task=$(aws ecs list-tasks --cluster "$CLUSTER" --service-name "$1" \
    --desired-status RUNNING --query 'taskArns[0]' --output text)
  [ "$task" = "None" ] || [ -z "$task" ] && return 1

  local eni
  eni=$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$task" \
    --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value | [0]" \
    --output text)
  [ "$eni" = "None" ] && return 1

  aws ec2 describe-network-interfaces --network-interface-ids "$eni" \
    --query 'NetworkInterfaces[0].Association.PublicIp' --output text
}

urls() {
  local fe be
  fe=$(task_ip "$FRONTEND" 2>/dev/null || true)
  be=$(task_ip "$BACKEND" 2>/dev/null || true)
  [ -n "${fe:-}" ] && [ "$fe" != "None" ] && echo "  app:  http://${fe}:3000"
  [ -n "${be:-}" ] && [ "$be" != "None" ] && {
    echo "  api:  http://${be}:8080/api"
    echo "  docs: http://${be}:8080/api/swagger-ui.html"
  }
  return 0
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
    echo "Down. About \$5/month keeps running: the stopped database's disk,"
    echo "two secrets, the pipeline, and stored images."
    ;;

  up)
    status=$(db_status)

    # RDS refuses to start an instance that is still shutting down, and a
    # "stopping" instance never reaches "available" on its own — so wait it
    # out rather than hanging later on the availability check.
    if [ "$status" = "stopping" ]; then
      echo "Database is still stopping; waiting for it to settle..."
      while [ "$(db_status)" = "stopping" ]; do sleep 15; done
      status=$(db_status)
    fi

    case "$status" in
      stopped)
        echo "Starting the database (takes a few minutes)..."
        aws rds start-db-instance --db-instance-identifier "$DB" \
          --query 'DBInstance.DBInstanceStatus' --output text
        ;;
      absent)
        echo "No database named '$DB' in $REGION — run terraform apply first." >&2
        exit 1
        ;;
      *)
        echo "Database is '$status'."
        ;;
    esac

    # The backend fails its health check without a reachable database, and
    # ECS would then kill the tasks — so wait for RDS before scaling up.
    echo "Waiting for the database to accept connections..."
    aws rds wait db-instance-available --db-instance-identifier "$DB"

    echo "Scaling services back up..."
    scale "$BACKEND" "$BACKEND_COUNT"
    scale "$FRONTEND" "$FRONTEND_COUNT"

    echo "Waiting for tasks to go healthy (the backend takes ~1 minute to boot)..."
    aws ecs wait services-stable --cluster "$CLUSTER" --services "$BACKEND" "$FRONTEND"

    echo
    echo "Up."
    urls
    ;;

  status)
    echo "Database: $(db_status)"
    aws ecs describe-services --cluster "$CLUSTER" --services "$BACKEND" "$FRONTEND" \
      --query 'services[].{service:serviceName,desired:desiredCount,running:runningCount}' \
      --output table
    urls
    ;;

  *)
    echo "usage: $0 {up|down|status}" >&2
    exit 2
    ;;
esac

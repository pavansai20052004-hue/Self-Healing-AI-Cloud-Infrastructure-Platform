#!/usr/bin/env bash
set -euo pipefail

service_name="${RENDER_SERVICE_NAME:-}"

run_java_service() {
  local module="$1"
  local jar_path
  jar_path="$(find "/workspace/${module}/target" -maxdepth 1 -name '*.jar' ! -name '*original*' | head -n 1)"

  if [[ -z "${jar_path}" ]]; then
    echo "No packaged jar found for ${module}" >&2
    exit 1
  fi

  exec java -jar "${jar_path}"
}

case "${service_name}" in
  monitoring-service*)
    run_java_service "monitoring-service"
    ;;
  healing-engine*)
    run_java_service "healing-engine"
    ;;
  incident-intelligence-service*)
    run_java_service "incident-intelligence-service"
    ;;
  ai-prediction-service*)
    cd /workspace/ai-prediction-service
    exec /opt/ai-venv/bin/python -m app.main
    ;;
  *)
    echo "Unknown RENDER_SERVICE_NAME=${service_name}" >&2
    exit 1
    ;;
esac

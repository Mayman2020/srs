#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Checking Docker availability..."
command -v docker >/dev/null 2>&1 || { echo "ERROR: docker not in PATH"; exit 1; }
docker compose version >/dev/null 2>&1 || { echo "ERROR: docker compose not available"; exit 1; }

ENV_FILE_ARGS=()
if [[ -f "${SCRIPT_DIR}/../env/.env" ]]; then
  ENV_FILE_ARGS+=(--env-file "${SCRIPT_DIR}/../env/.env")
  echo "Using deploy/env/.env"
elif [[ -f "${SCRIPT_DIR}/../../.env" ]]; then
  ENV_FILE_ARGS+=(--env-file "${SCRIPT_DIR}/../../.env")
  echo "Using repository root .env"
else
  echo "Warning: no deploy/env/.env or repo-root .env"
fi

echo "Validating docker-compose..."
docker compose "${ENV_FILE_ARGS[@]}" -f "${SCRIPT_DIR}/docker-compose.yml" config >/dev/null

echo "Starting stack..."
docker compose "${ENV_FILE_ARGS[@]}" -f "${SCRIPT_DIR}/docker-compose.yml" up --build

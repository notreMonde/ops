#!/usr/bin/env sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ -n "${OPS_MAVEN_CMD:-}" ] && [ -x "${OPS_MAVEN_CMD}" ]; then
  MVN_CMD="${OPS_MAVEN_CMD}"
elif command -v mvn >/dev/null 2>&1; then
  MVN_CMD="mvn"
else
  echo "Maven executable not found. Set OPS_MAVEN_CMD or install Maven." >&2
  exit 1
fi

exec "${MVN_CMD}" -f "${BASE_DIR}/pom.xml" "$@"

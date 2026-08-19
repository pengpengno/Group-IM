#!/usr/bin/env bash
set -euo pipefail

umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
INVENTORY_SQL="${SCRIPT_DIR}/tenant_schema_inventory.sql"
TENANT_SCHEMA="${TENANT_SCHEMA:-${1:-}}"
OUTPUT_ROOT="${SNAPSHOT_OUTPUT_DIR:-/tmp/group-im-tenant-schema-snapshots}"
TOOL_VERSION="1"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 2
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"
}

for command_name in psql pg_dump sha256sum python3 date; do
  require_command "$command_name"
done

[[ -f "$INVENTORY_SQL" ]] || fail "inventory SQL not found: $INVENTORY_SQL"
[[ -n "$TENANT_SCHEMA" ]] || fail "set TENANT_SCHEMA or pass the schema name as the first argument"
[[ "$TENANT_SCHEMA" =~ ^[A-Za-z0-9_]+$ ]] || fail "TENANT_SCHEMA must contain only letters, digits, and underscore"
[[ "${TENANT_SCHEMA,,}" != "public" ]] || fail "public is a control-plane schema, not a tenant snapshot target"

mkdir -p "$OUTPUT_ROOT"
OUTPUT_ROOT="$(cd "$OUTPUT_ROOT" && pwd -P)"

REPO_ROOT=""
if command -v git >/dev/null 2>&1; then
  REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || true)"
fi
if [[ -n "$REPO_ROOT" && ( "$OUTPUT_ROOT" == "$REPO_ROOT" || "$OUTPUT_ROOT" == "$REPO_ROOT"/* ) ]]; then
  fail "SNAPSHOT_OUTPUT_DIR must be outside the Git worktree to avoid accidental commits: $OUTPUT_ROOT"
fi

schema_exists="$(psql -X -Atq -v ON_ERROR_STOP=1 -c "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = '${TENANT_SCHEMA}')")"
[[ "$schema_exists" == "t" ]] || fail "tenant schema does not exist: $TENANT_SCHEMA"

GENERATED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
SNAPSHOT_ID="${TENANT_SCHEMA}-${STAMP}"
TMP_DIR="${OUTPUT_ROOT}/.${SNAPSHOT_ID}.tmp.$$"
FINAL_DIR="${OUTPUT_ROOT}/${SNAPSHOT_ID}"

mkdir -p "$TMP_DIR"
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

INVENTORY_FILE="${TMP_DIR}/inventory.json"
SCHEMA_SQL_FILE="${TMP_DIR}/schema.sql"
MANIFEST_FILE="${TMP_DIR}/manifest.json"

{
  printf 'BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;\n'
  printf 'SET LOCAL search_path TO "%s", public;\n' "$TENANT_SCHEMA"
  cat "$INVENTORY_SQL"
  printf 'COMMIT;\n'
} | psql -X -Atq -v ON_ERROR_STOP=1 -f - > "$INVENTORY_FILE"

python3 - "$INVENTORY_FILE" "$TENANT_SCHEMA" <<'PY'
import json
import sys

path, expected_schema = sys.argv[1], sys.argv[2]
with open(path, "r", encoding="utf-8") as handle:
    payload = json.load(handle)
if payload.get("format_version") != 1:
    raise SystemExit("unexpected inventory format_version")
if payload.get("schema") != expected_schema:
    raise SystemExit("inventory schema does not match requested tenant")
for required_key in (
    "tables",
    "columns",
    "constraints",
    "indexes",
    "views",
    "sequences",
    "triggers",
    "routines",
    "domains",
    "enum_labels",
):
    if not isinstance(payload.get(required_key), list):
        raise SystemExit(f"inventory field must be an array: {required_key}")
PY

pg_dump \
  --schema-only \
  --no-owner \
  --no-privileges \
  --quote-all-identifiers \
  --schema="$TENANT_SCHEMA" \
  --exclude-table="${TENANT_SCHEMA}.flyway_schema_history" \
  --exclude-table="${TENANT_SCHEMA}.tenant_schema_metadata" \
  > "$SCHEMA_SQL_FILE"

INVENTORY_SHA256="$(sha256sum "$INVENTORY_FILE" | awk '{print $1}')"
SCHEMA_SQL_SHA256="$(sha256sum "$SCHEMA_SQL_FILE" | awk '{print $1}')"
DATABASE_NAME="$(psql -X -Atq -v ON_ERROR_STOP=1 -c 'SELECT current_database()')"
DATABASE_FINGERPRINT="$(printf '%s' "$DATABASE_NAME" | sha256sum | awk '{print $1}')"
SERVER_VERSION="$(psql -X -Atq -v ON_ERROR_STOP=1 -c 'SHOW server_version')"
PSQL_VERSION="$(psql --version | head -n 1)"
PG_DUMP_VERSION="$(pg_dump --version | head -n 1)"

SNAPSHOT_SCHEMA="$TENANT_SCHEMA" \
SNAPSHOT_GENERATED_AT="$GENERATED_AT" \
SNAPSHOT_TOOL_VERSION="$TOOL_VERSION" \
SNAPSHOT_DATABASE_FINGERPRINT="$DATABASE_FINGERPRINT" \
SNAPSHOT_SERVER_VERSION="$SERVER_VERSION" \
SNAPSHOT_PSQL_VERSION="$PSQL_VERSION" \
SNAPSHOT_PG_DUMP_VERSION="$PG_DUMP_VERSION" \
SNAPSHOT_INVENTORY_SHA256="$INVENTORY_SHA256" \
SNAPSHOT_SCHEMA_SQL_SHA256="$SCHEMA_SQL_SHA256" \
python3 - "$MANIFEST_FILE" <<'PY'
import json
import os
import sys

manifest = {
    "format_version": 1,
    "tool_version": os.environ["SNAPSHOT_TOOL_VERSION"],
    "source_schema": os.environ["SNAPSHOT_SCHEMA"],
    "generated_at_utc": os.environ["SNAPSHOT_GENERATED_AT"],
    "database_name_sha256": os.environ["SNAPSHOT_DATABASE_FINGERPRINT"],
    "postgres_server_version": os.environ["SNAPSHOT_SERVER_VERSION"],
    "psql_version": os.environ["SNAPSHOT_PSQL_VERSION"],
    "pg_dump_version": os.environ["SNAPSHOT_PG_DUMP_VERSION"],
    "files": {
        "inventory.json": {"sha256": os.environ["SNAPSHOT_INVENTORY_SHA256"]},
        "schema.sql": {"sha256": os.environ["SNAPSHOT_SCHEMA_SQL_SHA256"]},
    },
    "notes": [
        "Schema-only snapshot; no application row data is exported.",
        "flyway_schema_history and tenant_schema_metadata are excluded from core schema evidence.",
        "Generated output is review input for #25 and must not be executed directly as a migration.",
    ],
}
with open(sys.argv[1], "w", encoding="utf-8") as handle:
    json.dump(manifest, handle, ensure_ascii=False, indent=2, sort_keys=True)
    handle.write("\n")
PY

mkdir "$FINAL_DIR"
mv "$INVENTORY_FILE" "$FINAL_DIR/inventory.json"
mv "$SCHEMA_SQL_FILE" "$FINAL_DIR/schema.sql"
mv "$MANIFEST_FILE" "$FINAL_DIR/manifest.json"
rmdir "$TMP_DIR"
trap - EXIT

printf 'Tenant schema snapshot created: %s\n' "$FINAL_DIR"
printf '  inventory sha256: %s\n' "$INVENTORY_SHA256"
printf '  schema.sql sha256: %s\n' "$SCHEMA_SQL_SHA256"
printf 'Review these files as baseline input; do not execute schema.sql directly.\n'

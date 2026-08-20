-- Non-destructive migration intentionally placed after the 2026081906 core baseline.
-- Existing tenants that are explicitly baselined at 2026081906 must execute this
-- migration successfully before they are considered managed by normal Flyway history.

CREATE TABLE IF NOT EXISTS tenant_schema_metadata (
    metadata_key VARCHAR(100) PRIMARY KEY,
    metadata_value TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO tenant_schema_metadata(metadata_key, metadata_value)
VALUES ('migration_runtime', 'group-im')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();

INSERT INTO tenant_schema_metadata(metadata_key, metadata_value)
VALUES ('core_baseline_contract', '2026081906')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();

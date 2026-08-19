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

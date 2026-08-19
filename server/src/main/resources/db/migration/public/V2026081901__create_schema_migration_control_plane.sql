CREATE TABLE IF NOT EXISTS public.schema_migration_run (
    run_id UUID PRIMARY KEY,
    mode VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    requested_by BIGINT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    total_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_schema_migration_run_mode
        CHECK (mode IN ('PLAN', 'APPLY')),
    CONSTRAINT ck_schema_migration_run_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'PARTIAL_FAILED', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS public.schema_migration_run_item (
    item_id BIGSERIAL PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES public.schema_migration_run(run_id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL,
    schema_name VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL,
    from_version VARCHAR(64),
    target_version VARCHAR(64),
    pending_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    duration_ms BIGINT,
    error_message VARCHAR(1000),
    CONSTRAINT uq_schema_migration_run_item UNIQUE (run_id, company_id),
    CONSTRAINT ck_schema_migration_run_item_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'PLANNED', 'SUCCEEDED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_schema_migration_run_requested_at
    ON public.schema_migration_run(requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_schema_migration_run_item_status
    ON public.schema_migration_run_item(run_id, status);

CREATE TABLE IF NOT EXISTS public.tenant_schema_state (
    company_id BIGINT PRIMARY KEY,
    schema_name VARCHAR(128) NOT NULL UNIQUE,
    current_version VARCHAR(64),
    target_version VARCHAR(64),
    status VARCHAR(24) NOT NULL,
    last_run_id UUID,
    last_error VARCHAR(1000),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tenant_schema_state_status
        CHECK (status IN ('UNKNOWN', 'UP_TO_DATE', 'PENDING', 'MIGRATING', 'FAILED', 'DRIFTED', 'DISABLED'))
);

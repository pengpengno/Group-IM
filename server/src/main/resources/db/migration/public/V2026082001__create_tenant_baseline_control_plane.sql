CREATE TABLE IF NOT EXISTS public.tenant_schema_preflight_state (
    company_id BIGINT PRIMARY KEY,
    schema_name VARCHAR(128) NOT NULL UNIQUE,
    classification VARCHAR(32) NOT NULL,
    baseline_version VARCHAR(64) NOT NULL,
    history_present BOOLEAN NOT NULL DEFAULT FALSE,
    observed_fingerprint VARCHAR(64),
    category_hashes JSONB NOT NULL DEFAULT '{}'::jsonb,
    repair_plan JSONB NOT NULL DEFAULT '[]'::jsonb,
    checked_by BIGINT,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tenant_schema_preflight_classification
        CHECK (classification IN ('BASELINE_READY', 'DRIFTED', 'CONFLICT', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_schema_preflight_classification
    ON public.tenant_schema_preflight_state(classification, checked_at DESC);

CREATE TABLE IF NOT EXISTS public.tenant_schema_baseline_audit (
    audit_id UUID PRIMARY KEY,
    company_id BIGINT NOT NULL,
    schema_name VARCHAR(128) NOT NULL,
    operator_user_id BIGINT,
    baseline_version VARCHAR(64) NOT NULL,
    expected_fingerprint VARCHAR(64),
    observed_fingerprint VARCHAR(64),
    status VARCHAR(24) NOT NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_tenant_schema_baseline_audit_status
        CHECK (status IN ('ATTEMPTED', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_schema_baseline_audit_company
    ON public.tenant_schema_baseline_audit(company_id, created_at DESC);

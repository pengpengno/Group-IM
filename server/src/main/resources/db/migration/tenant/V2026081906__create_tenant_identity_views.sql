-- Core tenant baseline: tenant-scoped projections of global identity tables.
-- The current tenant is bound by public.company.schema_name; no company ID is hard-coded.

DO $$
DECLARE
    tenant_schema TEXT := current_schema();
BEGIN
    EXECUTE format(
        'CREATE VIEW company AS '
        'SELECT c.company_id, c.active, c.created_at, c.name, c.schema_name, c.updated_at '
        'FROM public.company c WHERE c.schema_name = %L',
        tenant_schema
    );

    EXECUTE format(
        'CREATE VIEW company_user AS '
        'SELECT cu.id, cu.company_id, cu.status, cu.user_id '
        'FROM public.company_user cu '
        'JOIN public.company c ON c.company_id = cu.company_id '
        'WHERE c.schema_name = %L',
        tenant_schema
    );

    CREATE VIEW users AS
    SELECT
        u.user_id,
        u.created_at,
        u.email,
        u.force_password_change,
        u.password_hash,
        u.phone_number,
        u.primary_company_id,
        u.refresh_token,
        u.updated_at,
        u.user_status,
        u.username
    FROM public.users u
    WHERE EXISTS (
        SELECT 1
        FROM company_user cu
        WHERE cu.user_id = u.user_id
    );
END
$$;

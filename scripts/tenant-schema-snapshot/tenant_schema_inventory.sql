WITH params AS (
    SELECT current_schema() AS schema_name
),
managed_tables AS (
    SELECT
        c.oid,
        c.relname AS table_name,
        CASE c.relkind
            WHEN 'r' THEN 'TABLE'
            WHEN 'p' THEN 'PARTITIONED_TABLE'
            ELSE c.relkind::text
        END AS table_kind,
        obj_description(c.oid, 'pg_class') AS comment
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN params p ON p.schema_name = n.nspname
    WHERE c.relkind IN ('r', 'p')
      AND c.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
),
table_rows AS (
    SELECT
        mt.table_name,
        mt.table_kind,
        mt.comment
    FROM managed_tables mt
    ORDER BY mt.table_name
),
column_rows AS (
    SELECT
        mt.table_name,
        a.attnum AS ordinal_position,
        a.attname AS column_name,
        format_type(a.atttypid, a.atttypmod) AS data_type,
        a.attnotnull AS not_null,
        pg_get_expr(ad.adbin, ad.adrelid) AS default_expression,
        NULLIF(a.attidentity, '') AS identity_kind,
        NULLIF(a.attgenerated, '') AS generated_kind
    FROM managed_tables mt
    JOIN pg_attribute a ON a.attrelid = mt.oid
    LEFT JOIN pg_attrdef ad ON ad.adrelid = a.attrelid AND ad.adnum = a.attnum
    WHERE a.attnum > 0
      AND NOT a.attisdropped
    ORDER BY mt.table_name, a.attnum
),
constraint_rows AS (
    SELECT
        mt.table_name,
        con.conname AS constraint_name,
        CASE con.contype
            WHEN 'p' THEN 'PRIMARY_KEY'
            WHEN 'u' THEN 'UNIQUE'
            WHEN 'f' THEN 'FOREIGN_KEY'
            WHEN 'c' THEN 'CHECK'
            WHEN 'x' THEN 'EXCLUSION'
            ELSE con.contype::text
        END AS constraint_type,
        pg_get_constraintdef(con.oid, true) AS definition
    FROM managed_tables mt
    JOIN pg_constraint con ON con.conrelid = mt.oid
    ORDER BY mt.table_name, con.conname
),
index_rows AS (
    SELECT
        i.tablename AS table_name,
        i.indexname AS index_name,
        i.indexdef AS definition
    FROM pg_indexes i
    JOIN params p ON p.schema_name = i.schemaname
    WHERE i.tablename NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
    ORDER BY i.tablename, i.indexname
),
view_rows AS (
    SELECT
        v.viewname AS view_name,
        pg_get_viewdef(format('%I.%I', v.schemaname, v.viewname)::regclass, true) AS definition
    FROM pg_views v
    JOIN params p ON p.schema_name = v.schemaname
    ORDER BY v.viewname
),
sequence_rows AS (
    SELECT
        s.sequence_name,
        s.data_type,
        s.start_value,
        s.minimum_value,
        s.maximum_value,
        s.increment,
        s.cycle_option
    FROM information_schema.sequences s
    JOIN params p ON p.schema_name = s.sequence_schema
    ORDER BY s.sequence_name
),
trigger_rows AS (
    SELECT
        c.relname AS table_name,
        t.tgname AS trigger_name,
        pg_get_triggerdef(t.oid, true) AS definition
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN params p ON p.schema_name = n.nspname
    WHERE NOT t.tgisinternal
      AND c.relname NOT IN ('flyway_schema_history', 'tenant_schema_metadata')
    ORDER BY c.relname, t.tgname
),
enum_rows AS (
    SELECT
        typ.typname AS type_name,
        enum.enumsortorder AS sort_order,
        enum.enumlabel AS label
    FROM pg_type typ
    JOIN pg_namespace n ON n.oid = typ.typnamespace
    JOIN params p ON p.schema_name = n.nspname
    JOIN pg_enum enum ON enum.enumtypid = typ.oid
    ORDER BY typ.typname, enum.enumsortorder
)
SELECT jsonb_pretty(
    jsonb_build_object(
        'format_version', 1,
        'schema', (SELECT schema_name FROM params),
        'tables', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.table_name) FROM table_rows r), '[]'::jsonb),
        'columns', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.table_name, r.ordinal_position) FROM column_rows r), '[]'::jsonb),
        'constraints', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.table_name, r.constraint_name) FROM constraint_rows r), '[]'::jsonb),
        'indexes', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.table_name, r.index_name) FROM index_rows r), '[]'::jsonb),
        'views', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.view_name) FROM view_rows r), '[]'::jsonb),
        'sequences', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.sequence_name) FROM sequence_rows r), '[]'::jsonb),
        'triggers', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.table_name, r.trigger_name) FROM trigger_rows r), '[]'::jsonb),
        'enum_labels', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.type_name, r.sort_order) FROM enum_rows r), '[]'::jsonb)
    )
)::text;

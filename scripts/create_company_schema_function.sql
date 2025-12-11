DROP FUNCTION IF EXISTS create_or_sync_company_schema;
CREATE OR REPLACE FUNCTION create_or_sync_company_schema(schema_name text)
RETURNS void AS $$
DECLARE
    table_rec record;
    column_rec record;
    constraint_rec record;
    index_rec record;
    exists_column boolean;
    exists_constraint boolean;
BEGIN
    -- 创建 schema（如果不存在）
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

    ---------------------------------------------------------------------
    -- 一、创建 / 同步表结构
    ---------------------------------------------------------------------
    FOR table_rec IN
        SELECT tablename FROM pg_tables WHERE schemaname = 'public'
    LOOP
        -- 创建表（如果不存在）
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I.%I (LIKE public.%I INCLUDING ALL)',
            schema_name, table_rec.tablename, table_rec.tablename
        );

        -- 检查 public 表的字段是否缺失并补齐
        FOR column_rec IN
            SELECT * FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = table_rec.tablename
        LOOP
            SELECT EXISTS(
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = schema_name
                  AND table_name = table_rec.tablename
                  AND column_name = column_rec.column_name
            ) INTO exists_column;

            IF NOT exists_column THEN
                EXECUTE format(
                    'ALTER TABLE %I.%I ADD COLUMN %I %s',
                    schema_name,
                    table_rec.tablename,
                    column_rec.column_name,
                    column_rec.data_type
                );
            END IF;
        END LOOP;

    END LOOP;

    ---------------------------------------------------------------------
    -- 二、复制约束（PK/UK/Check/ForeignKey）
    -- 注意：唯一和主键的索引无需额外复制，会自动创建
    ---------------------------------------------------------------------
    FOR constraint_rec IN
        SELECT conname,
               contype,
               pg_get_constraintdef(oid) AS condef,
               conrelid::regclass AS tablename
        FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace
    LOOP
        SELECT EXISTS(
            SELECT 1
            FROM pg_constraint
            WHERE connamespace = schema_name::regnamespace
              AND conname = constraint_rec.conname
        ) INTO exists_constraint;

        IF NOT exists_constraint THEN
            EXECUTE format(
                'ALTER TABLE %I.%I ADD CONSTRAINT %I %s',
                schema_name,
                constraint_rec.tablename,
                constraint_rec.conname,
                constraint_rec.condef
            );
        END IF;
    END LOOP;

    ---------------------------------------------------------------------
    -- 三、复制普通索引（排除主键和唯一索引）
    ---------------------------------------------------------------------
    FOR index_rec IN
        SELECT indexname, indexdef
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND indexdef NOT LIKE '%PRIMARY KEY%'
          AND indexdef NOT LIKE '%UNIQUE%'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS %I.%I', schema_name, index_rec.indexname);

        EXECUTE replace(
                    replace(
                        index_rec.indexdef,
                        'public.',
                        schema_name || '.'
                    ),
                    ' ON ',
                    ' ON ' || schema_name || '.'
                 );
    END LOOP;

END;
$$ LANGUAGE plpgsql;




--  应用程序中 来触发即可
---- 创建触发器，当向company表插入数据时自动创建schema
-- 创建一个触发器函数，当在company表中插入新记录时自动创建schema
DROP FUNCTION IF EXISTS create_company_schema_trigger_func;
CREATE OR REPLACE FUNCTION create_company_schema_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    -- 调用创建schema的函数
    PERFORM create_or_sync_company_schema(NEW.schema_name);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--  应用程序中 来触发即可
---- 创建触发器，当向company表插入数据时自动创建schema
DROP TRIGGER IF EXISTS create_company_schema_trigger ON company;
CREATE TRIGGER create_company_schema_trigger
    AFTER INSERT ON company
    FOR EACH ROW
    EXECUTE FUNCTION create_company_schema_trigger_func();
-- SELECT create_or_sync_company_schema('company_tike2');

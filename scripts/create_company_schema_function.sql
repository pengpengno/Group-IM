DROP FUNCTION IF EXISTS create_or_sync_company_schema;
CREATE OR REPLACE FUNCTION create_or_sync_company_schema(schema_name text, company_id bigint)
RETURNS text AS $$
DECLARE
    table_rec record;
    column_rec record;
    constraint_rec record;
    index_rec record;
    exists_column boolean;
    exists_constraint boolean;
    -- 定义全局系统表列表，这些表不应复制到租户schema中
    system_tables text[] := array['users', 'company', 'company_user'];
BEGIN
    -- 创建 schema（如果不存在）
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

    ---------------------------------------------------------------------
    -- 一、创建 / 同步表结构
    ---------------------------------------------------------------------
    FOR table_rec IN
        SELECT tablename FROM pg_tables WHERE schemaname = 'public'
    LOOP
        -- 检查是否为系统表，如果是则跳过
        IF table_rec.tablename = ANY(system_tables) THEN
            CONTINUE;
        END IF;
        
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
    -- 二、为系统表创建视图（带数据过滤）
    ---------------------------------------------------------------------
    -- 为 company 表创建视图
    EXECUTE format('DROP VIEW IF EXISTS %I.company', schema_name);
    EXECUTE format(
        'CREATE VIEW %I.company AS SELECT * FROM public.company ' ||
        'WHERE company_id = %L',
        schema_name, company_id
    );

    -- 为 company_user 表创建视图
    EXECUTE format('DROP VIEW IF EXISTS %I.company_user', schema_name);
    EXECUTE format(
        'CREATE VIEW %I.company_user AS SELECT * FROM public.company_user ' ||
        'WHERE company_id = %L',
        schema_name, company_id
    );

    -- 为 users 表创建视图（使用当前schema中的company_user视图）
    EXECUTE format('DROP VIEW IF EXISTS %I.users', schema_name);
    EXECUTE format(
        'CREATE VIEW %I.users AS SELECT u.* FROM public.users u ' ||
        'WHERE EXISTS (SELECT 1 FROM %I.company_user cu ' ||
        'WHERE cu.user_id = u.user_id)',
        schema_name, schema_name
    );

    ---------------------------------------------------------------------
    -- 三、检查并补充可能缺失的约束
    ---------------------------------------------------------------------
    FOR constraint_rec IN
        SELECT conname,
               contype,
               pg_get_constraintdef(oid) AS condef,
               conrelid::regclass AS tablename
        FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace
    LOOP
        -- 跳过系统表的约束复制
        IF REPLACE(constraint_rec.tablename::text, '"', '') = ANY(system_tables) THEN
            CONTINUE;
        END IF;
        
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
    -- 四、检查并补充可能缺失的普通索引
    ---------------------------------------------------------------------
    FOR index_rec IN
        SELECT indexname, indexdef
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND indexdef NOT LIKE '%PRIMARY KEY%'
          AND indexdef NOT LIKE '%UNIQUE%'
    LOOP
        -- 检查索引是否属于系统表，如果是则跳过
        IF (
            SELECT REPLACE(SUBSTRING(index_rec.indexdef FROM 'ON ([^ \(]+)'), 'public.', '') = ANY(system_tables)
        ) THEN
            CONTINUE;
        END IF;
        
        -- 检查索引是否已存在
        SELECT EXISTS(
            SELECT 1
            FROM pg_indexes
            WHERE schemaname = schema_name
              AND indexname = index_rec.indexname
        ) INTO exists_constraint;
        
        -- 如果索引不存在，则创建它
        IF NOT exists_constraint THEN
            EXECUTE replace(
                        replace(
                            index_rec.indexdef,
                            'public.',
                            schema_name || '.'
                        ),
                        ' ON ',
                        ' ON ' || schema_name || '.'
                     );
        END IF;
    END LOOP;
    RETURN schema_name;
END;
$$ LANGUAGE plpgsql;



--  应用程序中 来触发即可
---- 创建触发器，当向company表插入数据时自动创建schema
-- 创建一个触发器函数，当在company表中插入新记录时自动创建schema
-- DROP FUNCTION IF EXISTS create_company_schema_trigger_func;
-- CREATE OR REPLACE FUNCTION create_company_schema_trigger_func()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     -- 调用创建schema的函数
--     PERFORM create_or_sync_company_schema(NEW.schema_name);
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- --  应用程序中 来触发即可
-- ---- 创建触发器，当向company表插入数据时自动创建schema
-- DROP TRIGGER IF EXISTS create_company_schema_trigger ON company;
-- CREATE TRIGGER create_company_schema_trigger
--     AFTER INSERT ON company
--     FOR EACH ROW
--     EXECUTE FUNCTION create_company_schema_trigger_func();

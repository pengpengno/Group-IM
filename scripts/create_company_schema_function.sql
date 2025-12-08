-- 创建一个函数，用于创建公司schema和完全复制表结构
CREATE OR REPLACE FUNCTION create_company_schema(schema_name TEXT)
RETURNS VOID AS $$
DECLARE
    table_name_var TEXT;
    seq_name_var TEXT;
    type_name_var TEXT;
    constraint_rec RECORD;
    index_rec RECORD;
    column_rec RECORD;
BEGIN
    -- 创建schema
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);
    
    -- 复制自定义数据类型（枚举等）
    FOR type_name_var IN
        SELECT t.typname
        FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = 'public'
          AND t.typtype = 'e'  -- enum types
    LOOP
        EXECUTE format('CREATE TYPE IF NOT EXISTS %I.%I AS ENUM (SELECT unnest(enum_range(NULL::public.%I)))',
                       schema_name, type_name_var, type_name_var);
    END LOOP;
    
    -- 复制public schema下的所有表结构到新的schema
    FOR table_name_var IN 
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
    LOOP
        -- 检查表是否已存在
        IF NOT EXISTS (
            SELECT 1 
            FROM information_schema.tables 
            WHERE table_schema = schema_name 
            AND table_name = table_name_var
        ) THEN
            -- 表不存在，创建新表
            EXECUTE format('CREATE TABLE %I.%I (LIKE public.%I INCLUDING ALL)',
                          schema_name, table_name_var, table_name_var);
        ELSE
            -- 表已存在，检查并添加缺失的列
            FOR column_rec IN
                SELECT 
                    pc.attname AS column_name,
                    pg_catalog.format_type(pc.atttypid, pc.atttypmod) AS column_type,
                    CASE WHEN pc.atthasdef THEN 'DEFAULT ' || pg_get_expr(pd.adbin, pd.adrelid) ELSE '' END AS default_value,
                    CASE WHEN pc.attnotnull THEN 'NOT NULL' ELSE 'NULL' END AS not_null
                FROM pg_attribute pc
                JOIN pg_class pcl ON pc.attrelid = pcl.oid
                JOIN pg_namespace pn ON pcl.relnamespace = pn.oid
                LEFT JOIN pg_attrdef pd ON pc.attrelid = pd.adrelid AND pc.attnum = pd.adnum
                WHERE pn.nspname = 'public' 
                AND pcl.relname = table_name_var
                AND pc.attnum > 0 
                AND NOT pc.attisdropped
                AND pc.attname NOT IN (
                    SELECT column_name 
                    FROM information_schema.columns 
                    WHERE table_schema = schema_name 
                    AND table_name = table_name_var
                )
            LOOP
                -- 添加缺失的列
                EXECUTE format('ALTER TABLE %I.%I ADD COLUMN %I %s %s %s',
                               schema_name, table_name_var, column_rec.column_name, 
                               column_rec.column_type, 
                               column_rec.default_value,
                               column_rec.not_null);
            END LOOP;
        END IF;
    END LOOP;
    
    -- 添加列检查约束
    FOR constraint_rec IN
        SELECT c.conname, cl.relname AS tablename, pg_get_constraintdef(c.oid) AS constraint_def
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        JOIN pg_class cl ON cl.oid = c.conrelid
        WHERE n.nspname = 'public'
          AND c.contype = 'c'  -- check constraints
    LOOP
        -- 先尝试删除同名约束（如果存在），然后再添加
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
                       schema_name, constraint_rec.tablename, constraint_rec.conname);
        EXECUTE format('ALTER TABLE %I.%I ADD CONSTRAINT %I %s',
                       schema_name, constraint_rec.tablename, constraint_rec.conname, constraint_rec.constraint_def);
    END LOOP;
    
    -- 添加主键约束
    FOR constraint_rec IN
        SELECT c.conname, cl.relname AS tablename, pg_get_constraintdef(c.oid) AS constraint_def
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        JOIN pg_class cl ON cl.oid = c.conrelid
        WHERE n.nspname = 'public'
          AND c.contype = 'p'  -- primary key constraints
    LOOP
        -- 先尝试删除同名约束（如果存在），然后再添加
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
                       schema_name, constraint_rec.tablename, constraint_rec.conname);
        EXECUTE format('ALTER TABLE %I.%I ADD CONSTRAINT %I %s',
                       schema_name, constraint_rec.tablename, constraint_rec.conname, constraint_rec.constraint_def);
    END LOOP;
    
    -- 添加唯一约束
    FOR constraint_rec IN
        SELECT c.conname, cl.relname AS tablename, pg_get_constraintdef(c.oid) AS constraint_def
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        JOIN pg_class cl ON cl.oid = c.conrelid
        WHERE n.nspname = 'public'
          AND c.contype = 'u'  -- unique constraints
    LOOP
        -- 先尝试删除同名约束（如果存在），然后再添加
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
                       schema_name, constraint_rec.tablename, constraint_rec.conname);
        EXECUTE format('ALTER TABLE %I.%I ADD CONSTRAINT %I %s',
                       schema_name, constraint_rec.tablename, constraint_rec.conname, constraint_rec.constraint_def);
    END LOOP;
    
    -- 添加外键约束
    FOR constraint_rec IN
        SELECT c.conname, cl.relname AS tablename, pg_get_constraintdef(c.oid) AS constraint_def
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        JOIN pg_class cl ON cl.oid = c.conrelid
        WHERE n.nspname = 'public'
          AND c.contype = 'f'  -- foreign key constraints
    LOOP
        -- 先尝试删除同名约束（如果存在），然后再添加
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
                       schema_name, constraint_rec.tablename, constraint_rec.conname);
        EXECUTE format('ALTER TABLE %I.%I ADD CONSTRAINT %I %s',
                       schema_name, constraint_rec.tablename, constraint_rec.conname, constraint_rec.constraint_def);
    END LOOP;
    
    -- 创建索引
    FOR index_rec IN
        SELECT indexname, tablename, indexdef
        FROM pg_indexes
        WHERE schemaname = 'public'
    LOOP
        -- 删除已存在的同名索引（如果存在）
        EXECUTE format('DROP INDEX IF EXISTS %I.%I', schema_name, index_rec.indexname);
        -- 替换schema名称并创建索引
        EXECUTE replace(replace(index_rec.indexdef, 'public.', schema_name || '.'), ' ON ', ' ON ' || schema_name || '.');
    END LOOP;
    
    -- 复制序列（sequences）
    FOR seq_name_var IN
        SELECT sequence_name
        FROM information_schema.sequences
        WHERE sequence_schema = 'public'
    LOOP
        EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.%I AS bigint INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE',
                      schema_name, seq_name_var);
    END LOOP;
    
    -- 如果有需要特殊处理的表数据，可以在这里添加
    -- 例如，一些配置表可能需要复制基础数据
    
END;
$$ LANGUAGE plpgsql;

-- 创建一个更新schema结构的函数，用于同步public schema的变更
CREATE OR REPLACE FUNCTION update_company_schema(schema_name TEXT)
RETURNS VOID AS $$
BEGIN
    -- 直接复用创建函数，因为它已经具备了幂等性和更新能力
    PERFORM create_company_schema(schema_name);
END;
$$ LANGUAGE plpgsql;

-- 创建一个批量更新所有公司schema的函数
CREATE OR REPLACE FUNCTION update_all_company_schemas()
RETURNS VOID AS $$
DECLARE
    schema_name_var TEXT;
BEGIN
    -- 遍历所有公司schema并更新它们
    FOR schema_name_var IN
        SELECT schema_name
        FROM company
        WHERE active = true
    LOOP
        PERFORM update_company_schema(schema_name_var);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 创建一个触发器函数，当在company表中插入新记录时自动创建schema
CREATE OR REPLACE FUNCTION create_company_schema_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    -- 调用创建schema的函数
    PERFORM create_company_schema(NEW.schema_name);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器，当向company表插入数据时自动创建schema
DROP TRIGGER IF EXISTS create_company_schema_trigger ON company;
CREATE TRIGGER create_company_schema_trigger
    AFTER INSERT ON company
    FOR EACH ROW
    EXECUTE FUNCTION create_company_schema_trigger_func();
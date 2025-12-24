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
    modified_condef TEXT;
    is_system_reference BOOLEAN;
    system_table_name TEXT;
    -- 定义全局系统表列表，这些表不应复制到租户schema中
    system_tables text[] := array[ 'company', 'company_user','users'];
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
    -- 删除系统表视图，使用 CASCADE 确保删除依赖关系
    -- 由于 users 视图依赖 company_user 视图，使用 CASCADE 可以一次性删除所有依赖
    EXECUTE format('DROP VIEW IF EXISTS %I.users CASCADE', schema_name);
    EXECUTE format('DROP VIEW IF EXISTS %I.company_user CASCADE', schema_name);
    EXECUTE format('DROP VIEW IF EXISTS %I.company CASCADE', schema_name);
    
    -- 重新创建系统表视图，按照依赖顺序创建：先创建被依赖的视图，再创建依赖其他视图的视图
    -- company 视图：过滤属于当前公司的公司数据
    EXECUTE format(
        'CREATE VIEW %I.company AS SELECT * FROM public.company ' ||
        'WHERE company_id = %L',
        schema_name, company_id
    );
    
    -- company_user 视图：过滤属于当前公司的用户-公司关联数据
    EXECUTE format(
        'CREATE VIEW %I.company_user AS SELECT * FROM public.company_user ' ||
        'WHERE company_id = %L',
        schema_name, company_id
    );
    
    -- users 视图：基于 company_user 视图过滤当前公司可访问的用户
    -- 使用 EXISTS 子查询确保只返回与当前公司有关联的用户
    EXECUTE format(
        'CREATE VIEW %I.users AS SELECT u.* FROM public.users u ' ||
        'WHERE EXISTS (SELECT 1 FROM %I.company_user cu ' ||
        'WHERE cu.user_id = u.user_id)',
        schema_name, schema_name
    );

    ---------------------------------------------------------------------
    -- 三、检查并补充可能缺失的约束（外键约束需要修改引用表为当前schema）
    ---------------------------------------------------------------------
    FOR constraint_rec IN
        SELECT conname,
               contype,
               pg_get_constraintdef(oid) AS condef,
               conrelid::regclass AS tablename,
               -- 获取被引用的表名（对于外键约束）
               confrelid::regclass::text AS referenced_table
        FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace
          AND contype = 'f'  -- 只处理外键约束
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
            modified_condef := constraint_rec.condef;
            
            is_system_reference := false;
            
            -- 检查是否引用了系统表
            FOREACH system_table_name IN ARRAY system_tables
            LOOP
                IF constraint_rec.referenced_table = system_table_name THEN
                    is_system_reference := true;
                    EXIT; -- 找到匹配项后退出循环
                END IF;
            END LOOP;
            
            -- 只处理引用租户表（非系统表）的外键约束
            IF NOT is_system_reference THEN
                -- 对于非系统表（租户表）的引用，确保引用到当前租户schema
                -- 检查约束定义中是否已经有schema前缀
                IF constraint_rec.condef LIKE '%REFERENCES %.%' THEN
                    -- 如果已经有schema前缀（但不是public，因为is_system_reference为false），需要替换为当前schema
                    -- 假设现有的schema前缀是public（在实际应用中可能需要更复杂的处理）
                    modified_condef := replace(
                        constraint_rec.condef,
                        'REFERENCES public.' || constraint_rec.referenced_table,
                        'REFERENCES ' || schema_name || '.' || constraint_rec.referenced_table
                    );
                ELSE
                    -- 如果没有schema前缀，需要添加当前租户schema前缀
                    -- 按可能的格式顺序尝试替换，一旦成功就停止
                    modified_condef := replace(
                        constraint_rec.condef,
                        ' REFERENCES ' || constraint_rec.referenced_table || '(',
                        ' REFERENCES ' || schema_name || '.' || constraint_rec.referenced_table || '('
                    );
                    
                    -- 如果上面的替换没有成功，尝试处理带空格的括号格式
                    IF modified_condef = constraint_rec.condef THEN
                        modified_condef := replace(
                            constraint_rec.condef,
                            ' REFERENCES ' || constraint_rec.referenced_table || ' (',
                            ' REFERENCES ' || schema_name || '.' || constraint_rec.referenced_table || ' ('
                        );
                    END IF;
                    
                    -- 如果上面的替换没有成功，尝试处理没有空格的格式
                    IF modified_condef = constraint_rec.condef THEN
                        modified_condef := replace(
                            constraint_rec.condef,
                            'REFERENCES ' || constraint_rec.referenced_table || '(',
                            'REFERENCES ' || schema_name || '.' || constraint_rec.referenced_table || '('
                        );
                    END IF;
                    
                    -- 如果上面的替换没有成功，尝试处理没有空格的格式（带空格的括号）
                    IF modified_condef = constraint_rec.condef THEN
                        modified_condef := replace(
                            constraint_rec.condef,
                            'REFERENCES ' || constraint_rec.referenced_table || ' (',
                            'REFERENCES ' || schema_name || '.' || constraint_rec.referenced_table || ' ('
                        );
                    END IF;
                END IF;
            END IF;
            
            EXECUTE format(
                'ALTER TABLE %I.%I ADD CONSTRAINT %I %s',
                schema_name,
                constraint_rec.tablename,
                constraint_rec.conname,
                modified_condef
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

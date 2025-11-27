-- 创建companies表
CREATE TABLE IF NOT EXISTS companies (
    company_id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    company_code VARCHAR(50) UNIQUE NOT NULL,
    schema_name VARCHAR(50) UNIQUE NOT NULL,
    contact_person VARCHAR(100),
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    address TEXT,
    status BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_companies_company_code ON companies(company_code);
CREATE INDEX IF NOT EXISTS idx_companies_schema_name ON companies(schema_name);

-- 插入示例数据
INSERT INTO companies (company_name, company_code, schema_name, contact_person, contact_email, contact_phone) 
VALUES 
    ('集团总公司', 'GROUP', 'company_group', '张总', 'zhang@group.com', '13800138000'),
    ('子公司A', 'COMPANY_A', 'company_a', '李经理', 'li@a.com', '13800138001'),
    ('子公司B', 'COMPANY_B', 'company_b', '王经理', 'wang@b.com', '13800138002')
ON CONFLICT (company_code) DO NOTHING;
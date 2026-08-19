package com.github.im.server.schema.migration.support;

import com.github.im.server.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SchemaNameValidator {
    private static final Pattern SAFE_SCHEMA = Pattern.compile("^[A-Za-z0-9_]+$");

    public String requireTenantSchema(String schemaName) {
        if (schemaName == null || schemaName.isBlank() || !SAFE_SCHEMA.matcher(schemaName).matches()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MIGRATION_INVALID_SCHEMA", "非法 tenant schema 名称");
        }
        if ("public".equalsIgnoreCase(schemaName)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MIGRATION_PUBLIC_NOT_TENANT", "public schema 只能通过 public bootstrap 管理");
        }
        return schemaName;
    }
}

package com.github.im.server.schema.migration.persistence;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.schema.migration.domain.TenantTarget;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.LinkedHashSet;
import java.util.List;

@Repository
public class TenantCatalogRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public TenantCatalogRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public List<TenantTarget> findAllActive() {
        return jdbc.query(
                "SELECT company_id, name, schema_name, active FROM public.company " +
                        "WHERE active = TRUE AND LOWER(schema_name) <> 'public' ORDER BY company_id",
                (rs, rowNum) -> new TenantTarget(
                        rs.getLong("company_id"),
                        rs.getString("name"),
                        rs.getString("schema_name"),
                        rs.getBoolean("active")
                )
        );
    }

    public List<TenantTarget> findActiveByIds(List<Long> rawCompanyIds) {
        if (rawCompanyIds == null || rawCompanyIds.isEmpty()) {
            return List.of();
        }
        List<Long> companyIds = new LinkedHashSet<>(rawCompanyIds).stream().toList();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", companyIds);
        List<TenantTarget> targets = jdbc.query(
                "SELECT company_id, name, schema_name, active FROM public.company " +
                        "WHERE active = TRUE AND LOWER(schema_name) <> 'public' AND company_id IN (:ids) ORDER BY company_id",
                params,
                (rs, rowNum) -> new TenantTarget(
                        rs.getLong("company_id"),
                        rs.getString("name"),
                        rs.getString("schema_name"),
                        rs.getBoolean("active")
                )
        );
        if (targets.size() != companyIds.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MIGRATION_INVALID_COMPANY_SCOPE",
                    "部分 companyId 不存在、未激活或属于 public，拒绝创建 tenant migration run");
        }
        return targets;
    }
}

package com.github.im.server.schema.migration.domain;

public enum TenantSchemaStateStatus {
    UNKNOWN,
    UP_TO_DATE,
    PENDING,
    MIGRATING,
    FAILED,
    DRIFTED,
    DISABLED
}

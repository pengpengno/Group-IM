package com.github.im.server.schema.migration.domain;

public enum MigrationRunStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    PARTIAL_FAILED,
    FAILED
}

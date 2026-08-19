package com.github.im.server.schema.migration.domain;

public enum MigrationItemStatus {
    QUEUED,
    RUNNING,
    PLANNED,
    SUCCEEDED,
    FAILED,
    SKIPPED
}

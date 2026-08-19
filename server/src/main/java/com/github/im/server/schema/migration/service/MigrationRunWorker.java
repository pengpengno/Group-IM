package com.github.im.server.schema.migration.service;

import com.github.im.server.schema.migration.domain.MigrationMode;
import com.github.im.server.schema.migration.domain.TenantMigrationPlan;
import com.github.im.server.schema.migration.domain.TenantTarget;
import com.github.im.server.schema.migration.persistence.MigrationRunRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class MigrationRunWorker {
    private final MigrationRunRepository runRepository;
    private final TenantMigrationExecutor migrationExecutor;
    private final TaskExecutor taskExecutor;

    public MigrationRunWorker(
            MigrationRunRepository runRepository,
            TenantMigrationExecutor migrationExecutor,
            @Qualifier("schemaMigrationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.runRepository = runRepository;
        this.migrationExecutor = migrationExecutor;
        this.taskExecutor = taskExecutor;
    }

    public void submit(UUID runId, MigrationMode mode, List<TenantTarget> targets) {
        taskExecutor.execute(() -> execute(runId, mode, targets));
    }

    void execute(UUID runId, MigrationMode mode, List<TenantTarget> targets) {
        runRepository.markRunRunning(runId);
        for (TenantTarget target : targets) {
            long startedNanos = System.nanoTime();
            try {
                if (mode == MigrationMode.PLAN) {
                    TenantMigrationPlan plan = migrationExecutor.plan(target.schemaName());
                    runRepository.markItemPlanned(runId, target, plan, elapsedMillis(startedNanos));
                } else {
                    TenantMigrationPlan before = migrationExecutor.plan(target.schemaName());
                    runRepository.markItemRunning(runId, target);
                    TenantMigrationPlan result = migrationExecutor.apply(target.schemaName());
                    runRepository.markItemSucceeded(
                            runId,
                            target,
                            before.currentVersion(),
                            result,
                            elapsedMillis(startedNanos)
                    );
                }
            } catch (Exception exception) {
                runRepository.markItemFailed(runId, target, safeMessage(exception), elapsedMillis(startedNanos));
            }
        }
        runRepository.finishRun(runId, mode);
    }

    private long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}

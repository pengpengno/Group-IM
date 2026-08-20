package com.github.im.server.workbench.common.audit;

import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.context.CurrentWorkContextProvider;
import com.github.im.server.workbench.common.context.WorkbenchTenantScope;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkbenchAuditService {

    private final CurrentWorkContextProvider contextProvider;
    private final WorkbenchAuditSink auditSink;

    public WorkbenchAuditService(CurrentWorkContextProvider contextProvider, WorkbenchAuditSink auditSink) {
        this.contextProvider = contextProvider;
        this.auditSink = auditSink;
    }

    public void recordCurrentActor(
            String category,
            String action,
            String resourceType,
            String resourceId,
            String beforeState,
            String afterState,
            Map<String, Object> metadata
    ) {
        CurrentWorkContext context = contextProvider.require();
        record(
                context.tenantScope(),
                context.userId(),
                category,
                action,
                resourceType,
                resourceId,
                beforeState,
                afterState,
                metadata
        );
    }

    public void record(
            WorkbenchTenantScope tenantScope,
            Long actorUserId,
            String category,
            String action,
            String resourceType,
            String resourceId,
            String beforeState,
            String afterState,
            Map<String, Object> metadata
    ) {
        auditSink.append(new WorkbenchAuditEvent(
                UUID.randomUUID(),
                tenantScope.companyId(),
                tenantScope.schemaName(),
                actorUserId,
                category,
                action,
                resourceType,
                resourceId,
                beforeState,
                afterState,
                Instant.now(),
                metadata
        ));
    }
}

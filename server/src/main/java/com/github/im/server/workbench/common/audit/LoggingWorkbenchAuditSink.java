package com.github.im.server.workbench.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingWorkbenchAuditSink implements WorkbenchAuditSink {

    @Override
    public void append(WorkbenchAuditEvent event) {
        log.info(
                "workbench_audit eventId={} companyId={} schema={} actorUserId={} category={} action={} resourceType={} resourceId={} before={} after={} metadata={}",
                event.eventId(),
                event.companyId(),
                event.schemaName(),
                event.actorUserId(),
                event.category(),
                event.action(),
                event.resourceType(),
                event.resourceId(),
                event.beforeState(),
                event.afterState(),
                event.metadata()
        );
    }
}

package com.github.im.server.workbench.common.audit;

public interface WorkbenchAuditSink {

    void append(WorkbenchAuditEvent event);
}

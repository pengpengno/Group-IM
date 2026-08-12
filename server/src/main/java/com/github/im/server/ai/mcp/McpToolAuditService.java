package com.github.im.server.ai.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Audit seam. Arguments are intentionally not logged; persistence follows with AutomationExecution. */
@Slf4j
@Service
public class McpToolAuditService {
    public void record(McpToolInvocation invocation, McpToolResult result, long elapsedMillis) {
        log.info("mcp_tool_invoked tool={} actor={} conversation={} success={} elapsedMs={} requestId={}",
                invocation.toolName(), invocation.context().actorUserId(), invocation.context().conversationId(),
                result.isSuccess(), elapsedMillis, invocation.context().requestId());
    }
}

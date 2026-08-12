package com.github.im.server.ai.mcp;

import org.springframework.stereotype.Service;

/** Single execution boundary shared by chat bots and automation workers. */
@Service
public class InternalMcpToolGateway {
    private final McpToolRegistry registry;
    private final ToolAccessPolicy accessPolicy;
    private final McpToolAuditService auditService;

    public InternalMcpToolGateway(McpToolRegistry registry, ToolAccessPolicy accessPolicy, McpToolAuditService auditService) {
        this.registry = registry;
        this.accessPolicy = accessPolicy;
        this.auditService = auditService;
    }

    public McpToolResult invoke(McpToolInvocation invocation) {
        long startedAt = System.nanoTime();
        try {
            McpToolHandler handler = registry.require(invocation.toolName());
            accessPolicy.requireAllowed(invocation, handler.descriptor());
            McpToolResult result = handler.execute(invocation);
            auditService.record(invocation, result, elapsedMillis(startedAt));
            return result;
        } catch (McpConfirmationRequiredException ex) {
            McpToolResult result = McpToolResult.error("CONFIRMATION_REQUIRED", "此操作需要你的确认后才能执行。");
            auditService.record(invocation, result, elapsedMillis(startedAt));
            return result;
        } catch (IllegalArgumentException ex) {
            McpToolResult result = McpToolResult.error("INVALID_ARGUMENT", "请求的机器人能力不可用或参数不正确。");
            auditService.record(invocation, result, elapsedMillis(startedAt));
            return result;
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}

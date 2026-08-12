package com.github.im.server.ai.mcp;

public interface McpToolHandler {
    McpToolDescriptor descriptor();

    McpToolResult execute(McpToolInvocation invocation);
}

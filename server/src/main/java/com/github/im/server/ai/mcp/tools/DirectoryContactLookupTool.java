package com.github.im.server.ai.mcp.tools;

import com.github.im.server.ai.mcp.*;
import com.github.im.server.ai.tool.UserInfoTool;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Sensitive contact lookup. The gateway requires an explicit confirmation. */
@Component
public class DirectoryContactLookupTool implements McpToolHandler {
    private final UserInfoTool userInfoTool;

    public DirectoryContactLookupTool(UserInfoTool userInfoTool) { this.userInfoTool = userInfoTool; }

    @Override public McpToolDescriptor descriptor() {
        return new McpToolDescriptor("directory.contact.lookup", "查询用户联系方式", McpToolRisk.SENSITIVE_READ,
                Map.of("type", "object", "required", java.util.List.of("query")));
    }

    @Override public McpToolResult execute(McpToolInvocation invocation) {
        String query = String.valueOf(invocation.arguments().getOrDefault("query", "")).trim();
        if (query.isBlank()) return McpToolResult.error("INVALID_ARGUMENT", "请提供联系人查询条件。");
        String text = userInfoTool.getUserContactInfo(query);
        return McpToolResult.success(Map.of("completed", true), text);
    }
}

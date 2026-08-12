package com.github.im.server.ai.mcp.tools;

import com.github.im.server.ai.mcp.McpToolDescriptor;
import com.github.im.server.ai.mcp.McpToolHandler;
import com.github.im.server.ai.mcp.McpToolInvocation;
import com.github.im.server.ai.mcp.McpToolResult;
import com.github.im.server.ai.mcp.McpToolRisk;
import com.github.im.server.ai.tool.UserInfoTool;
import org.springframework.stereotype.Component;

import java.util.Map;

/** First robot-callable capability. The legacy service remains the data source. */
@Component
public class DirectorySearchTool implements McpToolHandler {
    private final UserInfoTool userInfoTool;

    public DirectorySearchTool(UserInfoTool userInfoTool) { this.userInfoTool = userInfoTool; }

    @Override public McpToolDescriptor descriptor() {
        return new McpToolDescriptor("directory.search", "查询当前租户内的用户基础资料", McpToolRisk.READ_ONLY,
                Map.of("type", "object", "required", java.util.List.of("query")));
    }

    @Override public McpToolResult execute(McpToolInvocation invocation) {
        Object value = invocation.arguments().get("query");
        String query = value == null ? "" : String.valueOf(value).trim();
        if (query.isBlank()) return McpToolResult.error("INVALID_ARGUMENT", "请提供要查询的用户名称、邮箱或 ID。");
        String text = userInfoTool.getUserInfo(query);
        return McpToolResult.success(Map.of("query", query, "summary", text), text);
    }
}

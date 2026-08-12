package com.github.im.server.ai;

import com.github.im.server.ai.mcp.BotToolContext;
import com.github.im.server.ai.mcp.InternalMcpToolGateway;
import com.github.im.server.ai.mcp.McpToolInvocation;
import com.github.im.server.ai.mcp.McpToolResult;
import com.github.im.server.ai.mcp.McpToolRegistry;
import com.github.im.server.model.ApprovalRequest;
import com.github.im.server.service.AutomationApprovalService;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Routes robot commands through the controlled MCP gateway. */
@Component
public class AiToolBot implements BotHandler {

    private final AiChatService ai;
    private final McpToolRegistry mcpToolRegistry;
    private final InternalMcpToolGateway mcpToolGateway;
    private final AutomationApprovalService automationApprovalService;

    public AiToolBot(
            AiChatService ai,
            McpToolRegistry mcpToolRegistry,
            InternalMcpToolGateway mcpToolGateway,
            AutomationApprovalService automationApprovalService
    ) {
        this.ai = ai;
        this.mcpToolRegistry = mcpToolRegistry;
        this.mcpToolGateway = mcpToolGateway;
        this.automationApprovalService = automationApprovalService;
    }

    @Override
    public boolean canHandle(Message message) {
        // This router is dedicated to AI-bot requests. A standalone assistant must
        // accept ordinary natural-language questions, not just command-shaped input.
        return true;
    }

    @Override
    public BotReply handle(Message message, BotContext context) {
        String raw = stripMentionPrefix(message.getContent());
        String content = normalize(raw);
        if (content.isEmpty()) {
            return new BotReply("可以发送 /help 查看支持命令，或发送 /capabilities 查看当前能力。");
        }
        if (content.startsWith("/help")) {
            return new BotReply(helpText(), "markdown");
        }
        if (content.startsWith("/capabilities")) {
            return new BotReply(capabilitiesText(), "markdown");
        }
        if (content.startsWith("/card")) {
            return new BotReply("这里是一张机器人能力卡片。", "card", capabilityCard());
        }
        if (content.startsWith("/tool")) {
            return new BotReply("工具调用只能通过受控机器人指令执行。请使用 /user、/contact 或 /capabilities。 ");
        }
        if (content.startsWith("/user")) {
            return directorySearch(message, extractArgument(raw));
        }
        if (content.startsWith("/contact")) {
            return contactConfirmation(message, extractArgument(raw));
        }
        if (content.startsWith("/summary")) {
            return new BotReply("会话摘要能力正在接入中；当前可使用 /user、/contact、/capabilities。");
        }
        if (content.contains("能力") || content.contains("支持什么") || content.contains("webhook")) {
            return new BotReply(capabilitiesText(), "markdown");
        }
        if (containsUserLookupIntent(content)) {
            return directorySearch(message, extractLookupKeyword(raw));
        }
        if (containsContactLookupIntent(content)) {
            return contactConfirmation(message, extractLookupKeyword(raw));
        }
        return new BotReply(ai.chat(raw));
    }

    private BotReply directorySearch(Message message, String query) {
        McpToolResult result = mcpToolGateway.invoke(new McpToolInvocation(
                "directory.search",
                Map.of("query", query == null ? "" : query),
                new BotToolContext(message.getSenderId(), message.getConversationId(), null, message.getMessageId(), false)
        ));
        return new BotReply(result.displayText());
    }

    private BotReply contactConfirmation(Message message, String query) {
        ApprovalRequest approval = automationApprovalService.requestContactLookup(
                message.getSenderId(), message.getConversationId(), query, message.getMessageId());
        return new BotReply("查看联系人详情需要确认。", "card", Map.of(
                "title", "需确认：查看联系人详情",
                "summary", "将查询“" + (query == null ? "" : query) + "”的敏感联系方式。",
                "sections", java.util.List.of(Map.of("title", "风险说明", "text", "联系人详情属于敏感读取，确认后才会执行。")),
                "approvalId", String.valueOf(approval.getApprovalId()),
                "expiresAt", approval.getExpiresAt().toString(),
                "actions", java.util.List.of(Map.of(
                        "label", "确认操作",
                        "value", "CONTACT_LOOKUP_CONFIRMATION_REQUIRED",
                        "approvalId", String.valueOf(approval.getApprovalId())
                ))
        ));
    }

    private String helpText() {
        return """
                可用命令：
                /help 查看帮助
                /capabilities 查看已接入的受控能力
                /card 查看能力卡片
                /user <用户名|邮箱|ID> 查询用户基础资料
                /contact <用户名|邮箱|ID> 发起联系方式查询确认
                /summary 查看会话摘要能力状态

                所有工具请求均由 MCP 网关统一鉴权、审计；联系方式读取需要确认。
                """.strip();
    }

    private String capabilitiesText() {
        String tools = mcpToolRegistry.listDescriptors().stream()
                .map(descriptor -> "- " + descriptor.name() + "（" + descriptor.risk().name() + "）")
                .collect(Collectors.joining(System.lineSeparator()));
        return "当前已注册的受控 MCP 能力：" + System.lineSeparator() + tools
                + System.lineSeparator() + System.lineSeparator()
                + "联系方式属于敏感数据，必须先确认后读取。";
    }

    private Map<String, Object> capabilityCard() {
        return Map.of(
                "title", "机器人受控能力",
                "summary", "机器人消息和卡片均会持久化到会话消息流；工具调用统一经过 MCP 网关。",
                "sections", java.util.List.of(
                        Map.of("title", "目录查询", "text", "通过 directory.search 返回用户基础资料，不返回联系方式。"),
                        Map.of("title", "联系方式", "text", "使用 /contact 创建确认卡片，确认后才读取敏感信息。")
                ),
                "actions", java.util.List.of(
                        Map.of("label", "查看帮助", "value", "/help"),
                        Map.of("label", "查看能力", "value", "/capabilities")
                )
        );
    }

    private boolean containsUserLookupIntent(String content) {
        return content.contains("查用户") || content.contains("用户信息")
                || content.contains("user info") || content.contains("get user");
    }

    private boolean containsContactLookupIntent(String content) {
        return content.contains("查联系人") || content.contains("联系方式") || content.contains("contact")
                || content.contains("邮箱") || content.contains("手机号");
    }

    private String extractArgument(String raw) {
        if (raw == null) return "";
        String[] parts = raw.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1].trim() : "";
    }

    private String extractLookupKeyword(String raw) {
        if (raw == null) return "";
        return raw.trim()
                .replace("查用户", "").replace("用户信息", "").replace("查联系人", "")
                .replace("联系方式", "").replace("邮箱", "").replace("手机号", "")
                .replace("contact", "").replace("user info", "").replace("get user", "").trim();
    }

    private String stripMentionPrefix(String content) {
        if (content == null) return "";
        return content.trim()
                .replaceFirst("^@机器人\\s*", "")
                .replaceFirst("^@AI助手\\s*", "")
                .replaceFirst("^@AI\\s+助手\\s*", "")
                .replaceFirst("^@ai助手\\s*", "")
                .replaceFirst("^@ai\\s+助手\\s*", "").trim();
    }

    private String normalize(String content) {
        return content == null ? "" : content.trim().toLowerCase(Locale.ROOT);
    }
}

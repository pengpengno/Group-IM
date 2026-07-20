package com.github.im.server.ai;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AiToolBot implements BotHandler {

    private final AiChatService ai;
    private final ToolRegistry toolRegistry;

    public AiToolBot(AiChatService ai, ToolRegistry toolRegistry) {
        this.ai = ai;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public boolean canHandle(Message msg) {
        String raw = msg.getContent();
        String content = normalize(stripMentionPrefix(raw));
        return content.startsWith("/")
                || content.contains("用户")
                || content.contains("联系人")
                || content.contains("邮箱")
                || content.contains("手机号")
                || content.contains("webhook")
                || content.contains("能力")
                || content.contains("@机器人")
                || content.contains("@ai助手")
                || content.contains("@ai 助手")
                || content.contains("help")
                || content.contains("user info")
                || content.contains("contact");
    }

    @Override
    public BotReply handle(Message msg, BotContext ctx) {
        String raw = stripMentionPrefix(msg.getContent());
        String content = normalize(raw);
        if (content.isEmpty()) {
            return new BotReply("可以发送 /help 查看支持命令，或发送 /capabilities 查看当前能力。");
        }

        if (content.startsWith("/help")) {
            return new BotReply(String.valueOf(toolRegistry.invokeTool("help", new Object[]{})), "markdown");
        }

        if (content.startsWith("/capabilities")) {
            return new BotReply(String.valueOf(toolRegistry.invokeTool("capabilities", new Object[]{})), "markdown");
        }

        if (content.startsWith("/card")) {
            return new BotReply(
                    "这里是一张机器人能力卡片。",
                    "card",
                    toolRegistry.invokeTool("demoCard", new Object[]{})
            );
        }

        if (content.startsWith("/tool")) {
            return handleToolCommand(raw);
        }

        if (content.startsWith("/user")) {
            String query = extractArgument(raw);
            return new BotReply(String.valueOf(toolRegistry.invokeTool("userQuery", new Object[]{query})));
        }

        if (content.startsWith("/contact")) {
            String query = extractArgument(raw);
            return new BotReply(String.valueOf(toolRegistry.invokeTool("getUserContact", new Object[]{query})));
        }

        if (content.startsWith("/summary")) {
            return new BotReply("会话摘要能力正在接入中，当前可以先使用 /user、/contact、/capabilities。");
        }

        if (content.contains("能力") || content.contains("支持什么") || content.contains("webhook")) {
            return new BotReply(String.valueOf(toolRegistry.invokeTool("capabilities", new Object[]{})), "markdown");
        }

        if (containsUserLookupIntent(content)) {
            String query = extractLookupKeyword(raw);
            return new BotReply(String.valueOf(toolRegistry.invokeTool("userQuery", new Object[]{query})));
        }

        if (containsContactLookupIntent(content)) {
            String query = extractLookupKeyword(raw);
            return new BotReply(String.valueOf(toolRegistry.invokeTool("getUserContact", new Object[]{query})));
        }

        return new BotReply(ai.chat(raw));
    }

    private BotReply handleToolCommand(String raw) {
        String[] parts = raw == null ? new String[0] : raw.trim().split("\\s+", 3);
        if (parts.length < 2) {
            return new BotReply("请指定工具名称，例如：/tool userQuery 张三");
        }

        String toolName = parts[1];
        if (!toolRegistry.hasTool(toolName)) {
            return new BotReply("未知工具：" + toolName + "。可用工具：" + String.join(", ", toolRegistry.getToolNames()));
        }

        Object[] params = parts.length > 2 ? new Object[]{parts[2]} : new Object[]{};
        Object result = toolRegistry.invokeTool(toolName, params);
        if ("demoCard".equals(toolName)) {
            return new BotReply("这里是一张机器人能力卡片。", "card", result);
        }
        return new BotReply(String.valueOf(result));
    }

    private boolean containsUserLookupIntent(String content) {
        return content.contains("查用户")
                || content.contains("用户信息")
                || content.contains("user info")
                || content.contains("get user");
    }

    private boolean containsContactLookupIntent(String content) {
        return content.contains("查联系人")
                || content.contains("联系方式")
                || content.contains("contact")
                || content.contains("邮箱")
                || content.contains("手机号");
    }

    private String extractArgument(String raw) {
        if (raw == null) {
            return "";
        }
        String[] parts = raw.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1].trim() : "";
    }

    private String extractLookupKeyword(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .replace("查用户", "")
                .replace("用户信息", "")
                .replace("查联系人", "")
                .replace("联系方式", "")
                .replace("邮箱", "")
                .replace("手机号", "")
                .replace("contact", "")
                .replace("user info", "")
                .replace("get user", "")
                .trim();
    }

    private String stripMentionPrefix(String content) {
        if (content == null) {
            return "";
        }
        return content.trim()
                .replaceFirst("^@机器人\\s*", "")
                .replaceFirst("^@AI助手\\s*", "")
                .replaceFirst("^@AI\\s+助手\\s*", "")
                .replaceFirst("^@ai助手\\s*", "")
                .replaceFirst("^@ai\\s+助手\\s*", "")
                .trim();
    }

    private String normalize(String content) {
        return content == null ? "" : content.trim().toLowerCase(Locale.ROOT);
    }
}

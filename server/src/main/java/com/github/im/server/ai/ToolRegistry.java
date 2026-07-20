package com.github.im.server.ai;

import com.github.im.server.ai.tool.UserInfoTool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class ToolRegistry {

    private final Map<String, Function<Object[], Object>> tools = new LinkedHashMap<>();

    public ToolRegistry(UserInfoTool userInfoTool) {
        registerTool("userQuery", params -> {
            if (params.length == 0) {
                return "缺少用户查询参数。";
            }
            return userInfoTool.getUserInfo(String.valueOf(params[0]));
        });

        registerTool("getUserContact", params -> {
            if (params.length == 0) {
                return "缺少联系人查询参数。";
            }
            return userInfoTool.getUserContactInfo(String.valueOf(params[0]));
        });

        registerTool("chatHistory", params -> "聊天记录摘要能力正在接入中。");
        registerTool("demoCard", params -> Map.of(
                "title", "机器人能力卡片",
                "summary", "当前已经具备 webhook、富文本渲染、用户查询和联系方式查询能力。",
                "sections", List.of(
                        Map.of("title", "Webhook", "text", "支持 token 校验和 HMAC-SHA256 签名校验"),
                        Map.of("title", "移动端消息", "text", "支持 Markdown、HTML、链接、代码块渲染"),
                        Map.of("title", "@触发", "text", "支持 @机器人、@AI助手、@AI 助手 的触发式输入")
                ),
                "actions", List.of(
                        Map.of("label", "查看帮助", "value", "/help"),
                        Map.of("label", "查看能力", "value", "/capabilities")
                )
        ));
        registerTool("capabilities", params -> """
                当前机器人已支持：
                1. 用户信息查询：输入 /user 张三 或 “查用户 张三”
                2. 联系方式查询：输入 /contact 张三 或 “查联系人 张三”
                3. 富文本消息显示：移动端已支持 Markdown、链接、代码块、部分 HTML 渲染
                4. Webhook 接入：服务端支持 /api/ai-bot/webhook/{token}
                5. Webhook 安全校验：支持 token 和 HMAC-SHA256 签名
                6. 结构化卡片回复：支持 card metadata，Webhook 可转 actionCard
                7. 通用问答：未命中工具时会回退到 AI 对话

                正在完善中的能力：
                1. 聊天记录摘要 /summary
                2. 更丰富的卡片消息和结构化通知
                3. 组织级路由、群内真正的机器人订阅/分发
                """.strip());
        registerTool("help", params -> """
                可用命令：
                /help 查看帮助
                /capabilities 查看能力清单
                /card 查看卡片消息示例
                /user <用户名|邮箱|ID> 查询用户信息
                /contact <用户名|邮箱|ID> 查询联系方式
                /summary 查看摘要能力状态
                /tool <toolName> <args> 直接调用工具

                当前可用工具：
                userQuery
                getUserContact
                chatHistory
                capabilities
                demoCard

                Webhook：
                POST /api/ai-bot/webhook/{token}
                支持 content、prompt、query、text.content、markdown.text、html.content 等字段
                可选签名头：
                X-Group-Timestamp
                X-Group-Signature
                """.strip());
    }

    public void registerTool(String name, Function<Object[], Object> toolFunction) {
        tools.put(name, toolFunction);
    }

    public Object invokeTool(String name, Object[] params) {
        if (!tools.containsKey(name)) {
            throw new IllegalArgumentException("Tool not found: " + name);
        }
        return tools.get(name).apply(params);
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public Set<String> getToolNames() {
        return tools.keySet();
    }
}

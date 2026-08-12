package com.github.im.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.dto.message.MessageDTO;
import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import com.github.im.server.ai.BotReply;
import com.github.im.server.ai.MessageRouter;
import com.github.im.server.config.ai.AiProperties;
import com.github.im.server.model.User;
import com.github.im.server.service.BotIdentityService;
import com.github.im.server.service.MessageService;
import com.github.im.server.service.ConversationService;
import com.github.im.server.service.ConversationBotConfigService;
import com.github.im.server.repository.GroupMemberRepository;
import com.github.im.server.repository.MessageRepository;
import com.github.im.conversation.ConversationRes;
import com.github.im.dto.message.MessagePostRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-bot")
@CrossOrigin
public class AiBotController {

    private final MessageRouter messageRouter;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final BotIdentityService botIdentityService;
    private final ConversationService conversationService;
    private final GroupMemberRepository groupMemberRepository;
    private final ConversationBotConfigService conversationBotConfigService;
    private final MessageRepository messageRepository;

    @Autowired
    public AiBotController(
            @Qualifier("aiMessageRouter") MessageRouter messageRouter,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            MessageService messageService,
            BotIdentityService botIdentityService,
            ConversationService conversationService,
            GroupMemberRepository groupMemberRepository,
            ConversationBotConfigService conversationBotConfigService,
            MessageRepository messageRepository
    ) {
        this.messageRouter = messageRouter;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.messageService = messageService;
        this.botIdentityService = botIdentityService;
        this.conversationService = conversationService;
        this.groupMemberRepository = groupMemberRepository;
        this.conversationBotConfigService = conversationBotConfigService;
        this.messageRepository = messageRepository;
    }

    @PostMapping("/message")
    public ResponseEntity<BotReply> handleMessage(@RequestBody MessageDTO<?> message, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (message.getConversationId() == null || message.getConversationId() <= 0) {
            throw new IllegalArgumentException("conversationId is required");
        }
        if (groupMemberRepository.findByConversationIdAndUserId(message.getConversationId(), user.getUserId()).isEmpty()) {
            throw new SecurityException("You are not a member of this conversation");
        }
        // The authenticated principal is authoritative. Never route tool calls with a client-supplied sender id.
        message.setFromAccountId(user.getUserId());
        conversationBotConfigService.requireEnabledForGroup(message.getConversationId());
        User bot = botIdentityService.assistantFor(user);
        boolean standaloneAssistantConversation = message.getConversationId() != null && message.getConversationId() > 0
                && groupMemberRepository.findByConversationIdAndUserId(message.getConversationId(), bot.getUserId()).isPresent();
        if (standaloneAssistantConversation && messageRepository
                .findByConversation_ConversationIdAndClientMsgId(message.getConversationId(), message.getClientMsgId()).isEmpty()) {
            MessagePostRequest userMessage = new MessagePostRequest();
            userMessage.setConversationId(message.getConversationId());
            userMessage.setType(MessageType.TEXT);
            userMessage.setClientMsgId(message.getClientMsgId());
            userMessage.setContent(message.getContent());
            messageService.sendMessage(userMessage, user);
        }
        BotReply reply = messageRouter.route(message);
        if (message.getConversationId() != null && message.getConversationId() > 0) {
            MessagePostRequest replyMessage = new MessagePostRequest();
            replyMessage.setConversationId(message.getConversationId());
            replyMessage.setType("card".equalsIgnoreCase(reply.getMessageType()) ? MessageType.BOT_CARD : MessageType.TEXT);
            replyMessage.setClientMsgId("bot-" + message.getClientMsgId());
            replyMessage.setContent(toChatContent(reply));
            messageService.sendMessage(replyMessage, bot);
        }
        return ResponseEntity.ok(reply);
    }

    /** Returns the durable private conversation used by the standalone assistant entry. */
    @PostMapping("/conversation")
    public ResponseEntity<ConversationRes> getOrCreateConversation(@AuthenticationPrincipal User user) {
        User bot = botIdentityService.assistantFor(user);
        return ResponseEntity.ok(conversationService.createOrGetPrivateChat(user.getUserId(), bot.getUserId()));
    }

    private String toChatContent(BotReply reply) {
        if ("card".equalsIgnoreCase(reply.getMessageType()) && reply.getMetadata() != null) {
            try { return objectMapper.writeValueAsString(reply.getMetadata()); }
            catch (Exception ignored) { return reply.getContent(); }
        }
        return reply.getContent();
    }

    @PostMapping("/webhook/{token}")
    public ResponseEntity<?> handleWebhook(
            @PathVariable String token,
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request
    ) {
        if (!isWebhookAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("errcode", 403, "errmsg", "invalid webhook token"));
        }

        String bodyText = rawBody == null ? "" : rawBody;
        if (!isWebhookSignatureValid(headers, bodyText)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("errcode", 403, "errmsg", "invalid webhook signature"));
        }

        JsonNode body = parseBody(bodyText);
        String content = extractWebhookContent(body);
        if (content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("errcode", 400, "errmsg", "empty webhook content"));
        }

        MessageDTO<?> message = new MessageDTO<>();
        message.setConversationId(readLong(body, "conversationId", 0L));
        message.setFromAccountId(readLong(body, "fromAccountId", 0L));
        message.setClientMsgId(readText(body, "clientMsgId", "webhook-" + System.currentTimeMillis()));
        message.setType(MessageType.TEXT);
        message.setStatus(MessageStatus.SENT);
        message.setTimestamp(LocalDateTime.now());
        message.setContent(content);

        BotReply reply = messageRouter.route(message);
        return ResponseEntity.ok(toWebhookResponse(reply, request));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Bot service is running");
    }

    private boolean isWebhookAuthorized(String token) {
        AiProperties.Webhook webhook = aiProperties.getWebhook();
        if (!webhook.isEnabled()) {
            return false;
        }
        List<String> tokens = webhook.getTokens();
        return tokens == null || tokens.isEmpty() || tokens.contains(token);
    }

    private boolean isWebhookSignatureValid(Map<String, String> headers, String rawBody) {
        AiProperties.Webhook webhook = aiProperties.getWebhook();
        if (!webhook.isSignatureEnabled()) {
            return true;
        }

        String secret = webhook.getSecret();
        if (!StringUtils.hasText(secret)) {
            return false;
        }

        String signatureHeader = findHeader(headers, webhook.getSignatureHeader());
        String timestampHeader = findHeader(headers, webhook.getTimestampHeader());
        if (!StringUtils.hasText(signatureHeader) || !StringUtils.hasText(timestampHeader)) {
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException ex) {
            return false;
        }

        long drift = Math.abs(Instant.now().getEpochSecond() - timestamp);
        if (drift > webhook.getTimestampToleranceSeconds()) {
            return false;
        }

        String payload = timestampHeader.trim() + "\n" + rawBody;
        String expected = hmacSha256Hex(secret, payload);
        String normalizedSignature = normalizeSignature(signatureHeader);
        return expected.equalsIgnoreCase(normalizedSignature);
    }

    private String findHeader(Map<String, String> headers, String headerName) {
        if (!StringUtils.hasText(headerName)) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (headerName.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeSignature(String signature) {
        String value = signature == null ? "" : signature.trim();
        if (value.startsWith("sha256=")) {
            return value.substring("sha256=".length());
        }
        return value;
    }

    private JsonNode parseBody(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String extractWebhookContent(JsonNode body) {
        if (body == null || body.isNull()) {
            return "";
        }

        String directContent = readText(body, "content", "");
        if (!directContent.isBlank()) {
            return directContent;
        }

        String prompt = readText(body, "prompt", "");
        if (!prompt.isBlank()) {
            return prompt;
        }

        String query = readText(body, "query", "");
        if (!query.isBlank()) {
            return query;
        }

        String textContent = body.path("text").path("content").asText("");
        if (!textContent.isBlank()) {
            return textContent;
        }

        String markdownContent = body.path("markdown").path("text").asText("");
        if (!markdownContent.isBlank()) {
            return markdownContent;
        }

        String htmlContent = body.path("html").path("content").asText("");
        if (!htmlContent.isBlank()) {
            return htmlContent;
        }

        return body.path("msg").asText("");
    }

    private Map<String, Object> toWebhookResponse(BotReply reply, HttpServletRequest request) {
        String messageType = reply.getMessageType() == null ? "text" : reply.getMessageType().toLowerCase(Locale.ROOT);
        String content = reply.getContent() == null ? "" : reply.getContent();

        if ("card".equals(messageType)) {
            return toActionCardResponse(reply, request);
        }

        if ("markdown".equals(messageType)) {
            return Map.of(
                    "msgtype", "markdown",
                    "markdown", Map.of(
                            "title", content.length() > 20 ? content.substring(0, 20) : content,
                            "text", content
                    )
            );
        }

        return Map.of(
                "msgtype", "text",
                "text", Map.of("content", content)
        );
    }

    private Map<String, Object> toActionCardResponse(BotReply reply, HttpServletRequest request) {
        Map<String, Object> metadata = asMap(reply.getMetadata());
        String title = stringValue(metadata.get("title"), "机器人消息");
        String summary = stringValue(metadata.get("summary"), reply.getContent());
        String text = buildCardMarkdown(title, summary, metadata);
        List<Map<String, Object>> actions = asListOfMaps(metadata.get("actions"));

        Map<String, Object> actionCard = new LinkedHashMap<>();
        actionCard.put("title", title);
        actionCard.put("text", text);

        if (actions.size() == 1 && StringUtils.hasText(stringValue(actions.get(0).get("url"), ""))) {
            actionCard.put("singleTitle", stringValue(actions.get(0).get("label"), "查看详情"));
            actionCard.put("singleURL", absolutizeUrl(stringValue(actions.get(0).get("url"), ""), request));
        } else if (!actions.isEmpty()) {
            List<Map<String, String>> btns = new ArrayList<>();
            for (Map<String, Object> action : actions) {
                String url = stringValue(action.get("url"), "");
                if (!StringUtils.hasText(url)) {
                    continue;
                }
                btns.add(Map.of(
                        "title", stringValue(action.get("label"), "操作"),
                        "actionURL", absolutizeUrl(url, request)
                ));
            }
            if (!btns.isEmpty()) {
                actionCard.put("btnOrientation", "1");
                actionCard.put("btns", btns);
            }
        }

        return Map.of(
                "msgtype", "actionCard",
                "actionCard", actionCard
        );
    }

    private String buildCardMarkdown(String title, String summary, Map<String, Object> metadata) {
        StringBuilder builder = new StringBuilder();
        builder.append("### ").append(title).append("\n\n");
        if (StringUtils.hasText(summary)) {
            builder.append(summary).append("\n\n");
        }

        for (Map<String, Object> section : asListOfMaps(metadata.get("sections"))) {
            String sectionTitle = stringValue(section.get("title"), "");
            String sectionText = stringValue(section.get("text"), "");
            if (StringUtils.hasText(sectionTitle)) {
                builder.append("- **").append(sectionTitle).append("**");
                if (StringUtils.hasText(sectionText)) {
                    builder.append(": ").append(sectionText);
                }
                builder.append("\n");
            } else if (StringUtils.hasText(sectionText)) {
                builder.append("- ").append(sectionText).append("\n");
            }
        }

        List<Map<String, Object>> actions = asListOfMaps(metadata.get("actions"));
        if (!actions.isEmpty()) {
            builder.append("\n");
            for (Map<String, Object> action : actions) {
                String label = stringValue(action.get("label"), "操作");
                String value = stringValue(action.get("value"), "");
                String url = stringValue(action.get("url"), "");
                if (StringUtils.hasText(url)) {
                    builder.append("- [").append(label).append("](").append(url).append(")\n");
                } else if (StringUtils.hasText(value)) {
                    builder.append("- ").append(label).append(": `").append(value).append("`\n");
                } else {
                    builder.append("- ").append(label).append("\n");
                }
            }
        }
        return builder.toString().trim();
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    private List<Map<String, Object>> asListOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                result.add(normalized);
            }
        }
        return result;
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }

    private String absolutizeUrl(String url, HttpServletRequest request) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();
        if (!(request.getScheme().equals("http") && port == 80) && !(request.getScheme().equals("https") && port == 443)) {
            base += ":" + port;
        }
        return url.startsWith("/") ? base + url : base + "/" + url;
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate webhook signature", ex);
        }
    }

    private long readLong(JsonNode body, String fieldName, long defaultValue) {
        if (body == null) {
            return defaultValue;
        }
        JsonNode node = body.get(fieldName);
        return node != null && node.canConvertToLong() ? node.asLong() : defaultValue;
    }

    private String readText(JsonNode body, String fieldName, String defaultValue) {
        if (body == null) {
            return defaultValue;
        }
        JsonNode node = body.get(fieldName);
        return node != null && !node.isNull() ? node.asText(defaultValue) : defaultValue;
    }
}

package com.github.im.server.ai;

/**
 * 机器人回复模型
 */
public class BotReply {
    private String content;
    private String messageType;
    private Object metadata;

    public BotReply() {}

    public BotReply(String content) {
        this.content = content;
        this.messageType = "text";
    }

    public BotReply(String content, String messageType) {
        this.content = content;
        this.messageType = messageType;
    }

    public BotReply(String content, String messageType, Object metadata) {
        this.content = content;
        this.messageType = messageType;
        this.metadata = metadata;
    }

    // Getter和Setter方法
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}
package com.github.im.server.ai;

import java.util.Map;
import java.util.HashMap;

/**
 * 机器人上下文
 */
public class BotContext {
    private Map<String, Object> attributes;

    public BotContext() {
        this.attributes = new HashMap<>();
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public <T> T getAttribute(String key, Class<T> type) {
        return type.cast(this.attributes.get(key));
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
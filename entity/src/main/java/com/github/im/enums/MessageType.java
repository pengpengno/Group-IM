package com.github.im.enums;

public enum MessageType {
    TEXT(0),
    FILE(1),
    STREAM(2),
    VIDEO(3),
    MARKDOWN(5);

    private final int code;

    MessageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MessageType fromCode(int code) {
        for (MessageType type : MessageType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null; // Default or throw exception
    }
}


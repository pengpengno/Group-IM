package com.github.im.enums;

public enum MessageStatus {
    REJECT(0),
    OFFLINE(1),
    SENTFAIL(7),
    HISTORY(10),
    READ(3),
    UNREAD(2),
    SENT(5),
    UNSENT(11);

    private final int code;

    MessageStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MessageStatus fromCode(int code) {
        for (MessageStatus status : MessageStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null; // Default or throw exception
    }
}
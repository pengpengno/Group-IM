package com.github.im.enums;

public enum MessageType {
    TEXT,
    FILE,
    VOICE,
    VIDEO,
    IMAGE,
    MEDIA,
    MEETING,
    /** Versioned robot action card JSON stored in Message.content. */
    BOT_CARD,
    /** Versioned Workbench/OA event card JSON stored in Message.content. */
    WORKBENCH
}

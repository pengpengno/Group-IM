package com.github.im.server.model;

import lombok.Data;

@Data
public class WebrtcMessage {
    private MessageType type;
    private String from;
    private String to;
    private String payload;
    private Long timestamp;

    public enum MessageType {
        OFFER,
        ANSWER,
        ICE_CANDIDATE,
        HANGUP,
        REJECT,
        HEARTBEAT
    }

    public WebrtcMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public WebrtcMessage(MessageType type, String from, String to, String payload) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
}
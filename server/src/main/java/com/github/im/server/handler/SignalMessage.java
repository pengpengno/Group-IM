package com.github.im.server.handler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SignalMessage {
    private String type;
    private String fromUser;
    private String toUser;
    private String sdp;
    private Map<String, Object> candidate;

    public String getType() { return type; }
    public String getFromUser() { return fromUser; }
    public String getToUser() { return toUser; }
    public String getSdp() { return sdp; }
    public Map<String, Object> getCandidate() { return candidate; }
}

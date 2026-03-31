package com.github.im.server.handler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SignalMessage {
    private String type;
    private String fromUser;
    private String fromUserName;
    private String fromAvatar;
    private String toUser;
    private String roomId;
    private String sdp;
    private String sdpType;
    private String reason;
    private List<Map<String, Object>> participants;
    private Map<String, Object> candidate;

    public String getType() { return type; }
    public String getFromUser() { return fromUser; }
    public String getFromUserName() { return fromUserName; }
    public String getFromAvatar() { return fromAvatar; }
    public String getToUser() { return toUser; }
    public String getRoomId() { return roomId; }
    public String getSdp() { return sdp; }
    public String getSdpType() { return sdpType; }
    public String getReason() { return reason; }
    public List<Map<String, Object>> getParticipants() { return participants; }
    public Map<String, Object> getCandidate() { return candidate; }
}

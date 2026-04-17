package com.github.im.dto.meeting;

import lombok.Data;

import java.util.List;

@Data
public class MeetingCreateRequest {
    private Long conversationId;
    private String roomId;
    private String title;
    private List<Long> participantIds;
    private Boolean recordMessage;
    private String scheduledAt; // ISO-8601 string or LocalDateTime
}

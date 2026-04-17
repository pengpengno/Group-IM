package com.github.im.dto.message;

import lombok.Data;

import java.util.List;

@Data
public class MeetingMessagePayLoad implements MessagePayLoad {
    private Long meetingId;
    private String roomId;
    private String title;
    private String action;
    private Long hostId;
    private java.time.LocalDateTime scheduledAt;
    private List<Long> participantIds;
    private Integer participantCount;
}

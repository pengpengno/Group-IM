package com.github.im.dto.message;

import lombok.Data;

import java.util.List;

@Data
public class MeetingMessagePayLoad implements MessagePayLoad {
    private Long meetingId;
    private String roomId;
    private String title;
    private String action;
    private String category;
    private String status;
    private String summary;
    private Long hostId;
    private Long actorId;
    private java.time.LocalDateTime scheduledAt;
    private Integer durationSeconds;
    private List<Long> participantIds;
    private Integer participantCount;
}

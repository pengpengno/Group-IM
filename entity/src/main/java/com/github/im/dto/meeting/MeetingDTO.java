package com.github.im.dto.meeting;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MeetingDTO {
    private Long meetingId;
    private Long conversationId;
    private String roomId;
    private String title;
    private Long hostId;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<MeetingParticipantDTO> participants;
}

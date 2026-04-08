package com.github.im.dto.meeting;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingParticipantDTO {
    private Long userId;
    private String username;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}

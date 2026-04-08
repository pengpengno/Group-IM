package com.github.im.dto.meeting;

import lombok.Data;

@Data
public class MeetingEndRequest {
    private String roomId;
    private Boolean recordMessage;
}

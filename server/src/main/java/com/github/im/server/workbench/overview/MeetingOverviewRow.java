package com.github.im.server.workbench.overview;

import com.github.im.server.model.enums.MeetingStatus;

import java.time.LocalDateTime;

public record MeetingOverviewRow(
        Long meetingId,
        String title,
        String roomId,
        MeetingStatus status,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}

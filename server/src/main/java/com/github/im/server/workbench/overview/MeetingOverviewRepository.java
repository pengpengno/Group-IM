package com.github.im.server.workbench.overview;

import com.github.im.server.model.Meeting;
import com.github.im.server.model.enums.MeetingParticipantStatus;
import com.github.im.server.model.enums.MeetingStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MeetingOverviewRepository extends Repository<Meeting, Long> {

    @Query("""
            SELECT new com.github.im.server.workbench.overview.MeetingOverviewRow(
                m.meetingId,
                m.title,
                m.roomId,
                m.status,
                COALESCE(m.scheduledAt, m.startedAt),
                m.endedAt
            )
            FROM Meeting m
            JOIN m.participants p
            WHERE p.user.userId = :userId
              AND p.status IN :participantStatuses
              AND m.status IN :meetingStatuses
              AND (
                    (m.scheduledAt >= :dayStart AND m.scheduledAt < :dayEnd)
                    OR (
                        m.scheduledAt IS NULL
                        AND m.startedAt >= :dayStart
                        AND m.startedAt < :dayEnd
                    )
              )
            ORDER BY COALESCE(m.scheduledAt, m.startedAt), m.meetingId
            """)
    List<MeetingOverviewRow> findTodayForParticipant(
            @Param("userId") Long userId,
            @Param("participantStatuses") Collection<MeetingParticipantStatus> participantStatuses,
            @Param("meetingStatuses") Collection<MeetingStatus> meetingStatuses,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd
    );
}

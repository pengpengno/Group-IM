package com.github.im.server.workbench.overview;

import com.github.im.dto.workbench.overview.WorkbenchOverviewDTO;
import com.github.im.server.model.enums.MeetingParticipantStatus;
import com.github.im.server.model.enums.MeetingStatus;
import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.permission.WorkbenchPermission;
import com.github.im.server.workbench.common.permission.WorkbenchPermissionService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkbenchOverviewServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-20T02:30:00Z"), ZONE);

    @Test
    void buildsStableOverviewFromCurrentContextAndMeetingProjection() {
        CurrentWorkContext context = new CurrentWorkContext(42L, "alice", 7L, "Acme", "tenant_a");
        AtomicReference<Long> queriedUser = new AtomicReference<>();
        AtomicReference<LocalDateTime> queriedStart = new AtomicReference<>();
        AtomicReference<LocalDateTime> queriedEnd = new AtomicReference<>();

        WorkbenchPermissionService permissionService = new WorkbenchPermissionService() {
            @Override
            public CurrentWorkContext require(WorkbenchPermission permission) {
                assertEquals(WorkbenchPermission.VIEW_WORKBENCH, permission);
                return context;
            }

            @Override
            public boolean isAllowed(WorkbenchPermission permission) {
                return true;
            }
        };

        MeetingOverviewRepository repository = (userId, participantStatuses, meetingStatuses, dayStart, dayEnd) -> {
            queriedUser.set(userId);
            queriedStart.set(dayStart);
            queriedEnd.set(dayEnd);
            assertTrue(meetingStatuses.contains(MeetingStatus.SCHEDULED));
            assertTrue(meetingStatuses.contains(MeetingStatus.ACTIVE));
            assertFalse(meetingStatuses.contains(MeetingStatus.ENDED));
            assertTrue(participantStatuses.contains(MeetingParticipantStatus.INVITED));
            assertTrue(participantStatuses.contains(MeetingParticipantStatus.JOINED));
            assertTrue(participantStatuses.contains(MeetingParticipantStatus.LEFT));
            assertFalse(participantStatuses.contains(MeetingParticipantStatus.REJECTED));
            return List.of(
                    new MeetingOverviewRow(
                            99L,
                            "产品同步会",
                            "room-99",
                            MeetingStatus.SCHEDULED,
                            LocalDateTime.of(2026, 8, 20, 15, 0),
                            null
                    )
            );
        };

        WorkbenchOverviewService service = new WorkbenchOverviewService(permissionService, repository, FIXED_CLOCK);
        WorkbenchOverviewDTO result = service.getOverview();

        assertEquals(42L, queriedUser.get());
        assertEquals(LocalDateTime.of(2026, 8, 20, 0, 0), queriedStart.get());
        assertEquals(LocalDateTime.of(2026, 8, 21, 0, 0), queriedEnd.get());
        assertEquals(7L, result.currentCompany().companyId());
        assertEquals("Acme", result.currentCompany().name());
        assertEquals(0, result.todoSummary().assignedTaskCount());
        assertEquals(0, result.todoSummary().overdueTaskCount());
        assertEquals(0, result.todoSummary().pendingApprovalCount());
        assertEquals(0, result.todoSummary().unreadAnnouncementCount());
        assertTrue(result.recentTasks().isEmpty());
        assertTrue(result.pendingApprovals().isEmpty());
        assertTrue(result.announcements().isEmpty());
        assertEquals(1, result.todaySchedules().size());
        assertEquals("MEETING", result.todaySchedules().getFirst().type());
        assertEquals(99L, result.todaySchedules().getFirst().resourceId());
        assertEquals("产品同步会", result.todaySchedules().getFirst().title());
        assertEquals(4, result.quickApps().size());
    }

    @Test
    void givesUntitledMeetingsASafeDisplayTitle() {
        CurrentWorkContext context = new CurrentWorkContext(42L, "alice", 7L, "Acme", "tenant_a");
        WorkbenchPermissionService permissionService = permissionService(context);
        MeetingOverviewRepository repository = (userId, participantStatuses, meetingStatuses, dayStart, dayEnd) -> List.of(
                new MeetingOverviewRow(
                        100L,
                        " ",
                        "room-100",
                        MeetingStatus.ACTIVE,
                        LocalDateTime.of(2026, 8, 20, 10, 0),
                        null
                )
        );

        WorkbenchOverviewDTO result = new WorkbenchOverviewService(permissionService, repository, FIXED_CLOCK).getOverview();

        assertEquals("会议", result.todaySchedules().getFirst().title());
    }

    private WorkbenchPermissionService permissionService(CurrentWorkContext context) {
        return new WorkbenchPermissionService() {
            @Override
            public CurrentWorkContext require(WorkbenchPermission permission) {
                return context;
            }

            @Override
            public boolean isAllowed(WorkbenchPermission permission) {
                return true;
            }
        };
    }
}

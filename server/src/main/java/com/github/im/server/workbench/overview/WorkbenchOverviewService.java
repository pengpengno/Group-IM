package com.github.im.server.workbench.overview;

import com.github.im.dto.workbench.overview.WorkbenchAnnouncementSummaryDTO;
import com.github.im.dto.workbench.overview.WorkbenchApprovalSummaryDTO;
import com.github.im.dto.workbench.overview.WorkbenchCompanySummaryDTO;
import com.github.im.dto.workbench.overview.WorkbenchOverviewDTO;
import com.github.im.dto.workbench.overview.WorkbenchQuickAppDTO;
import com.github.im.dto.workbench.overview.WorkbenchScheduleSummaryDTO;
import com.github.im.dto.workbench.overview.WorkbenchTodoSummaryDTO;
import com.github.im.server.model.enums.MeetingParticipantStatus;
import com.github.im.server.model.enums.MeetingStatus;
import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.permission.WorkbenchPermission;
import com.github.im.server.workbench.common.permission.WorkbenchPermissionService;
import com.github.im.server.workbench.task.service.TaskOverviewProjection;
import com.github.im.server.workbench.task.service.TaskOverviewQueryService;
import com.github.im.server.workbench.approval.service.ApprovalOverviewQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
public class WorkbenchOverviewService {

    private static final EnumSet<MeetingStatus> OVERVIEW_MEETING_STATUSES =
            EnumSet.of(MeetingStatus.SCHEDULED, MeetingStatus.ACTIVE);

    private static final EnumSet<MeetingParticipantStatus> OVERVIEW_PARTICIPANT_STATUSES =
            EnumSet.of(
                    MeetingParticipantStatus.INVITED,
                    MeetingParticipantStatus.JOINED,
                    MeetingParticipantStatus.LEFT
            );

    private static final List<WorkbenchQuickAppDTO> QUICK_APPS = List.of(
            new WorkbenchQuickAppDTO("TASK", "任务"),
            new WorkbenchQuickAppDTO("MEETING", "会议"),
            new WorkbenchQuickAppDTO("CONTACTS", "通讯录"),
            new WorkbenchQuickAppDTO("AUTOMATION", "自动化"),
            new WorkbenchQuickAppDTO("SETTINGS", "设置")
    );

    private final WorkbenchPermissionService permissionService;
    private final MeetingOverviewRepository meetingOverviewRepository;
    private final TaskOverviewQueryService taskOverviewQueryService;
    private final Clock clock;
    private final ApprovalOverviewQueryService approvalOverviewQueryService;

    public WorkbenchOverviewService(
            WorkbenchPermissionService permissionService,
            MeetingOverviewRepository meetingOverviewRepository,
            TaskOverviewQueryService taskOverviewQueryService,
            Clock workbenchClock,
            ApprovalOverviewQueryService approvalOverviewQueryService
    ) {
        this.permissionService = permissionService;
        this.meetingOverviewRepository = meetingOverviewRepository;
        this.taskOverviewQueryService = taskOverviewQueryService;
        this.clock = workbenchClock;
        this.approvalOverviewQueryService = approvalOverviewQueryService;
    }

    @Transactional(readOnly = true)
    public WorkbenchOverviewDTO getOverview() {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.VIEW_WORKBENCH);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        TaskOverviewProjection taskProjection = taskOverviewQueryService.query(context.userId(), now);
        var approvalProjection = approvalOverviewQueryService.query(context.userId());
        List<WorkbenchScheduleSummaryDTO> schedules = meetingOverviewRepository.findTodayForParticipant(
                        context.userId(),
                        OVERVIEW_PARTICIPANT_STATUSES,
                        OVERVIEW_MEETING_STATUSES,
                        dayStart,
                        dayEnd
                ).stream()
                .map(this::toSchedule)
                .toList();

        return new WorkbenchOverviewDTO(
                new WorkbenchCompanySummaryDTO(context.companyId(), context.companyName()),
                new WorkbenchTodoSummaryDTO(
                        taskProjection.assignedTaskCount(),
                        taskProjection.overdueTaskCount(),
                        approvalProjection.pendingCount(),
                        0
                ),
                taskProjection.recentTasks(),
                approvalProjection.recent(),
                schedules,
                List.<WorkbenchAnnouncementSummaryDTO>of(),
                QUICK_APPS
        );
    }

    private WorkbenchScheduleSummaryDTO toSchedule(MeetingOverviewRow row) {
        String title = row.title();
        if (title == null || title.isBlank()) {
            title = "会议";
        }
        return new WorkbenchScheduleSummaryDTO(
                "MEETING",
                row.meetingId(),
                title,
                row.status().name(),
                row.startsAt(),
                row.endsAt()
        );
    }
}

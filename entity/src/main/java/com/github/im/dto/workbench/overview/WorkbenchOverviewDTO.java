package com.github.im.dto.workbench.overview;

import java.util.List;

public record WorkbenchOverviewDTO(
        WorkbenchCompanySummaryDTO currentCompany,
        WorkbenchTodoSummaryDTO todoSummary,
        List<WorkbenchTaskSummaryDTO> recentTasks,
        List<WorkbenchApprovalSummaryDTO> pendingApprovals,
        List<WorkbenchScheduleSummaryDTO> todaySchedules,
        List<WorkbenchAnnouncementSummaryDTO> announcements,
        List<WorkbenchQuickAppDTO> quickApps
) {
    public WorkbenchOverviewDTO {
        recentTasks = List.copyOf(recentTasks);
        pendingApprovals = List.copyOf(pendingApprovals);
        todaySchedules = List.copyOf(todaySchedules);
        announcements = List.copyOf(announcements);
        quickApps = List.copyOf(quickApps);
    }
}

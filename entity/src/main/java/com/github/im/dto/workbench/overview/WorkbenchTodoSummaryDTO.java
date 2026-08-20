package com.github.im.dto.workbench.overview;

public record WorkbenchTodoSummaryDTO(
        long assignedTaskCount,
        long overdueTaskCount,
        long pendingApprovalCount,
        long unreadAnnouncementCount
) {
    public static WorkbenchTodoSummaryDTO empty() {
        return new WorkbenchTodoSummaryDTO(0, 0, 0, 0);
    }
}

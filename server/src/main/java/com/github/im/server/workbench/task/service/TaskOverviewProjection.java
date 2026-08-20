package com.github.im.server.workbench.task.service;

import com.github.im.dto.workbench.overview.WorkbenchTaskSummaryDTO;

import java.util.List;

public record TaskOverviewProjection(
        long assignedTaskCount,
        long overdueTaskCount,
        List<WorkbenchTaskSummaryDTO> recentTasks
) {
    public TaskOverviewProjection {
        recentTasks = List.copyOf(recentTasks);
    }
}

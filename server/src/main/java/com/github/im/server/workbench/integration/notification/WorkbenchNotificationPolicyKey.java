package com.github.im.server.workbench.integration.notification;

import com.github.im.dto.workbench.notification.WorkbenchCategory;

public record WorkbenchNotificationPolicyKey(
        WorkbenchCategory category,
        String action
) {
    public WorkbenchNotificationPolicyKey {
        if (category == null) {
            throw new IllegalArgumentException("Workbench notification category is required");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Workbench notification action is required");
        }
    }
}

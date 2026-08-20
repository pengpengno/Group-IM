package com.github.im.dto.workbench.notification;

public record WorkbenchTarget(
        Long companyId,
        WorkbenchCategory category,
        String resourceId
) {
}

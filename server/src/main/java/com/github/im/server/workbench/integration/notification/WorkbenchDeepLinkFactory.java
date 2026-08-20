package com.github.im.server.workbench.integration.notification;

import com.github.im.dto.workbench.notification.WorkbenchCategory;
import com.github.im.dto.workbench.notification.WorkbenchTarget;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WorkbenchDeepLinkFactory {

    public String create(WorkbenchTarget target) {
        if (target == null || target.category() == null || target.companyId() == null || target.resourceId() == null) {
            throw new IllegalArgumentException("Workbench target is incomplete");
        }
        return UriComponentsBuilder.newInstance()
                .scheme("group")
                .host("workbench")
                .pathSegment(categoryPath(target.category()), target.resourceId())
                .queryParam("companyId", target.companyId())
                .build()
                .encode()
                .toUriString();
    }

    private String categoryPath(WorkbenchCategory category) {
        return switch (category) {
            case TASK -> "task";
            case APPROVAL -> "approval";
            case ANNOUNCEMENT -> "announcement";
            case SCHEDULE -> "schedule";
            case REPORT -> "report";
        };
    }
}

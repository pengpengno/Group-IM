package com.github.im.server.workbench.integration.notification;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Single fail-closed decision point used by future Workbench emitters. */
@Component
public class WorkbenchRolloutGate {

    private final WorkbenchRolloutProperties properties;

    public WorkbenchRolloutGate(WorkbenchRolloutProperties properties) {
        this.properties = properties;
    }

    public boolean allows(WorkbenchDeliveryChannel channel) {
        return status(channel).allowed();
    }

    public Status status(WorkbenchDeliveryChannel channel) {
        List<String> blockers = new ArrayList<>();
        if (!properties.isEnabled()) {
            blockers.add("master switch is disabled");
        }
        if (isBlank(properties.getMinimumWebVersion())) {
            blockers.add("minimum web/electron version is not recorded");
        }
        if (isBlank(properties.getMinimumAndroidVersion())) {
            blockers.add("minimum android version is not recorded");
        }
        if (!channelEnabled(channel)) {
            blockers.add(channel.name().toLowerCase() + " switch is disabled");
        }
        return new Status(blockers.isEmpty(), List.copyOf(blockers));
    }

    private boolean channelEnabled(WorkbenchDeliveryChannel channel) {
        return switch (channel) {
            case CLIENT_EVENT -> properties.isClientEventEnabled();
            case PUSH -> properties.isPushEnabled();
            case IM_CARD -> properties.isImCardEnabled();
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Status(boolean allowed, List<String> blockers) {
    }
}

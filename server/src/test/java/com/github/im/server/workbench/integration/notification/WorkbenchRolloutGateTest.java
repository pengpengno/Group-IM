package com.github.im.server.workbench.integration.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkbenchRolloutGateTest {

    @Test
    void defaultsFailClosed() {
        WorkbenchRolloutGate gate = new WorkbenchRolloutGate(new WorkbenchRolloutProperties());

        assertFalse(gate.allows(WorkbenchDeliveryChannel.CLIENT_EVENT));
        assertFalse(gate.allows(WorkbenchDeliveryChannel.PUSH));
        assertFalse(gate.allows(WorkbenchDeliveryChannel.IM_CARD));
    }

    @Test
    void masterSwitchAndBothReleaseFloorsAreRequired() {
        WorkbenchRolloutProperties properties = readyProperties();
        properties.setMinimumAndroidVersion("");
        WorkbenchRolloutGate gate = new WorkbenchRolloutGate(properties);

        assertFalse(gate.allows(WorkbenchDeliveryChannel.CLIENT_EVENT));
        assertTrue(gate.status(WorkbenchDeliveryChannel.CLIENT_EVENT).blockers().stream()
                .anyMatch(value -> value.contains("android")));
    }

    @Test
    void channelsCanBeRolledOutAndRolledBackIndependently() {
        WorkbenchRolloutProperties properties = readyProperties();
        properties.setClientEventEnabled(true);
        WorkbenchRolloutGate gate = new WorkbenchRolloutGate(properties);

        assertTrue(gate.allows(WorkbenchDeliveryChannel.CLIENT_EVENT));
        assertFalse(gate.allows(WorkbenchDeliveryChannel.PUSH));
        assertFalse(gate.allows(WorkbenchDeliveryChannel.IM_CARD));

        properties.setEnabled(false);
        assertFalse(gate.allows(WorkbenchDeliveryChannel.CLIENT_EVENT));
    }

    private WorkbenchRolloutProperties readyProperties() {
        WorkbenchRolloutProperties properties = new WorkbenchRolloutProperties();
        properties.setEnabled(true);
        properties.setMinimumWebVersion("release-containing-pr-52");
        properties.setMinimumAndroidVersion("release-containing-pr-59");
        return properties;
    }
}

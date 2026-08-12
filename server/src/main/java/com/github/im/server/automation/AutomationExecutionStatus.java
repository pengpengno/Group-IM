package com.github.im.server.automation;

/** Lifecycle of an automation action. */
public enum AutomationExecutionStatus {
    PENDING_APPROVAL,
    APPROVED,
    DECLINED,
    EXECUTED,
    FAILED
}

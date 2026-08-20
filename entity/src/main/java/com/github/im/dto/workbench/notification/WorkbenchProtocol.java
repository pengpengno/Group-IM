package com.github.im.dto.workbench.notification;

public final class WorkbenchProtocol {
    public static final int VERSION_1 = 1;
    public static final int MAX_RESOURCE_ID_LENGTH = 128;
    public static final int MAX_ACTION_LENGTH = 64;
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_SUMMARY_LENGTH = 300;
    public static final int MAX_FALLBACK_TEXT_LENGTH = 300;
    public static final int MAX_STATUS_LENGTH = 32;

    private WorkbenchProtocol() {
    }
}

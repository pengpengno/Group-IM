package com.github.im.server.workbench.common.integration;

import java.util.UUID;

/**
 * Attachment boundary for Workbench domains. Tenant routing must already be
 * established before this adapter is called. Domain-specific visibility rules
 * are layered on top of this tenant-local availability check.
 */
public interface FileAdapter {

    boolean isAvailable(UUID fileId);

    WorkbenchFileRef requireAvailable(UUID fileId);
}

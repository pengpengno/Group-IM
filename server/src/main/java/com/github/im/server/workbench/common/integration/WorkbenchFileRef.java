package com.github.im.server.workbench.common.integration;

import java.util.UUID;

public record WorkbenchFileRef(
        UUID id,
        String originalName,
        String contentType,
        long size
) {
}

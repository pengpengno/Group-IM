package com.github.im.server.workbench.common.integration;

public record OrganizationMemberRef(
        Long userId,
        String username,
        String email
) {
}

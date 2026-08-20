package com.github.im.server.workbench.common.integration;

public interface OrganizationAdapter {

    boolean isActiveMember(Long companyId, Long userId);

    OrganizationMemberRef requireActiveMember(Long companyId, Long userId);
}

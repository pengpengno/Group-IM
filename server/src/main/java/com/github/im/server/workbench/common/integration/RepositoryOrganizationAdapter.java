package com.github.im.server.workbench.common.integration;

import com.github.im.server.model.CompanyUser;
import com.github.im.server.model.User;
import com.github.im.server.repository.CompanyUserRepository;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.springframework.stereotype.Component;

@Component
public class RepositoryOrganizationAdapter implements OrganizationAdapter {

    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public RepositoryOrganizationAdapter(
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository
    ) {
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean isActiveMember(Long companyId, Long userId) {
        if (companyId == null || userId == null) {
            return false;
        }
        return companyUserRepository.findByUserIdAndCompanyId(userId, companyId)
                .filter(link -> link.getStatus() == CompanyUser.CompanyUserStatus.ACTIVE)
                .flatMap(link -> userRepository.findById(userId))
                .map(User::isEnabled)
                .orElse(false);
    }

    @Override
    public OrganizationMemberRef requireActiveMember(Long companyId, Long userId) {
        if (!isActiveMember(companyId, userId)) {
            throw WorkbenchException.notFound(
                    WorkbenchErrorCode.MEMBER_NOT_FOUND,
                    "当前公司中不存在可用成员: " + userId
            );
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> WorkbenchException.notFound(
                        WorkbenchErrorCode.MEMBER_NOT_FOUND,
                        "当前公司中不存在成员: " + userId
                ));
        return new OrganizationMemberRef(user.getUserId(), user.getUsername(), user.getEmail());
    }
}

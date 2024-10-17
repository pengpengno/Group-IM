package com.github.im.server.repository;

import com.github.im.server.model.UserPrivacySetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPrivacySettingRepository extends JpaRepository<UserPrivacySetting, Long> {
    UserPrivacySetting findByUserUserId(Long userId);
}

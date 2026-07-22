package com.github.im.server.repository;

import com.github.im.server.model.SystemConfigItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemConfigItemRepository extends JpaRepository<SystemConfigItem, Long> {

    Optional<SystemConfigItem> findByConfigKey(String configKey);

    List<SystemConfigItem> findAllByConfigGroupOrderByConfigKeyAsc(String configGroup);
}

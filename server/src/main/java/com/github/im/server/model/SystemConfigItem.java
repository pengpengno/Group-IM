package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "system_config_item")
public class SystemConfigItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 120)
    private String configKey;

    @Column(name = "config_group", nullable = false, length = 60)
    private String configGroup;

    @Column(name = "config_value", nullable = false, length = 255)
    private String configValue;

    @Column(name = "value_type", nullable = false, length = 30)
    private String valueType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "default_value", nullable = false, length = 255)
    private String defaultValue;

    @Column(name = "public_readable", nullable = false)
    private Boolean publicReadable = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

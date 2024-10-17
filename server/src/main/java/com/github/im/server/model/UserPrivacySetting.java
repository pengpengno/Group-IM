package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_privacy_settings")
public class UserPrivacySetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settingId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    private User user;

    private Boolean visibilityStatus = true;
    private Boolean allowFriendRequest = true;
    private Boolean showLastOnlineTime = true;

    @Column(nullable = false)
    private String notificationPreference = "all";
}

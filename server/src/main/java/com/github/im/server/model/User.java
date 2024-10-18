package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Transient
    private String password;


    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String passwordHash;

    private String avatarUrl;

    private String bio;

    @Column(nullable = false)
    private boolean status = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}

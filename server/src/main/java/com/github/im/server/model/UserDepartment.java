package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_departments")
@Data
public class UserDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public UserDepartment() {}

    public UserDepartment(Long userId, Long departmentId) {
        this.userId = userId;
        this.departmentId = departmentId;
    }
    
    // Getters and setters
    // ... existing getters and setters ...
}
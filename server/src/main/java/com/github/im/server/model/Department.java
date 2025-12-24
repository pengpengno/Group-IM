package com.github.im.server.model;

import com.github.im.dto.user.UserBasicInfo;
import com.github.im.dto.user.UserInfo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "departments")
@Data
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "order_num")
    private Integer orderNum;

    @Column(nullable = false)
    private Boolean status = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    private List<Department> children;

    @Transient
    private List<UserBasicInfo> members;
    
    /**
     * 属于该部门的用户列表
     */
    @ManyToMany(mappedBy = "departments", fetch = FetchType.LAZY)
    private List<User> users;

    // Getters and Setters
    // ... existing getters and setters ...
}
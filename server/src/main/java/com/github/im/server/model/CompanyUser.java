package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "company_user")
@Data
public class CompanyUser  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "company_id")
    private Long companyId;


    // 用户在该公司的状态 (例如: ACTIVE, DISABLED)
    @Enumerated(EnumType.STRING)
    private CompanyUserStatus status = CompanyUserStatus.ACTIVE;

    public enum CompanyUserStatus {
        ACTIVE,
        DISABLED,
    }

}
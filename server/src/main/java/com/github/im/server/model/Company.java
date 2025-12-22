package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/***
 * 公司实体 这张表 的数据默认放置 在 public schema 下
 *
 */
@Entity
@Table(name = "company" )
@Data
public class Company implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "schema_name", nullable = false, unique = true)
    private String schemaName;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 关联的用户列表
     */
    @ManyToMany(mappedBy = "companies", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    public Company() {
    }
    public Company(String name, String schemaName) {
        this.name = name;
        this.schemaName = schemaName;
    }
}
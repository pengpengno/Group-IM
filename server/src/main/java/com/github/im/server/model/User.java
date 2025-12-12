package com.github.im.server.model;

import com.github.im.server.model.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@Table(name = "users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    // account
    private String username;

    @Column()
    private String email;

    @Transient
    private String password;

    @Column()
    private String phoneNumber;

    @Column(nullable = false)
    private String passwordHash;

    private String avatarUrl;

    private String bio;

    /**
     * 长期 TOKEN
     */
    @Column(length = 1024)
    private String refreshToken;

    /**
     * 账户状态 false 是的话
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean status = true;

    @Enumerated(EnumType.STRING)
    private Status userStatus;

    /**
     * 标记用户是否需要在下次登录时修改密码
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean forcePasswordChange = false;

    // 添加公司ID字段
    @Column(name = "company_ids")
    private List<Long> companyIds;

    // 主公司ID
    @Column(name = "primary_company_id")
    private Long primaryCompanyId;
    
    /**
     * 当前登录的 公司Id 
     */
    @Transient
    private Long currentLoginCompanyId;


//    // 添加部门ID字段
//    @Column(name = "department_id")
//    private Long departmentId;

    private LocalDateTime createdAt ;
    private LocalDateTime updatedAt ;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onPersist() {
        var now = LocalDateTime.now();
        this.updatedAt = now;
        this.userStatus = Status.ACTIVE;
        this.status = true;
        this.createdAt = now;
    }

    public String getAccount() {
        return getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    public String getPassword(){
        return getPasswordHash();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return Status.ACTIVE.equals(getUserStatus()) ;
//        return true;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return authorities;
    }
}
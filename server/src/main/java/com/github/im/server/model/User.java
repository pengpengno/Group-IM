package com.github.im.server.model;

import com.github.im.server.model.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;

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

    @Enumerated(EnumType.STRING)
    private Status userStatus;


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
//        return Status.ACTIVE.equals(getUserStatus()) ;
        return true;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
//        TODo
        return null;
    }
}

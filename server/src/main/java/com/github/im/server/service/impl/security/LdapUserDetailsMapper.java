package com.github.im.server.service.impl.security;

import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.time.LocalDateTime;
import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class LdapUserDetailsMapper implements UserDetailsContextMapper , ContextMapper<UserDetails> {

    private final UserRepository userRepository;


    @Override
    public UserDetails mapFromContext(Object ctx) throws NamingException {
        if (ctx instanceof DirContextAdapter) {
            DirContextAdapter adapter = (DirContextAdapter) ctx;
            String username = adapter.getStringAttribute("uid");
            String email = extractEmail(adapter, username);
            String phoneNumber = extractPhoneNumber(adapter);

            // 检查本地数据库中是否已存在该用户
            User user = userRepository.findByUsernameOrEmail(username).orElse(null);

            if (user == null) {
                // 如果本地数据库中不存在该用户，则创建一个新用户
                log.info("Creating new local user account for LDAP user: {}", username);
                user = User.builder()
                        .username(username)
                        .email(email)
                        .phoneNumber(phoneNumber)
                        .passwordHash("") // LDAP用户不需要本地密码
                        .avatarUrl("")
                        .bio("")
                        .status(true)
                        .forcePasswordChange(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                user = userRepository.save(user);
                log.info("Created new local user account for LDAP user: {}", username);
                return user;
            }
        }
        return null;
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {
        // 首先检查本地数据库中是否已存在该用户
        User user = userRepository.findByUsernameOrEmail(username).orElse(null);
        
        if (user == null) {
            // 如果本地数据库中不存在该用户，则创建一个新用户
            log.info("Creating new local user account for LDAP user: {}", username);
            
            user = User.builder()
                    .username(username)
                    .email(extractEmail(ctx, username))
                    .phoneNumber(extractPhoneNumber(ctx))
                    .passwordHash("") // LDAP用户不需要本地密码
                    .avatarUrl("")
                    .bio("")
                    .status(true)
                    .forcePasswordChange(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            user = userRepository.save(user);
        }
        
        // 创建并返回UserDetails对象
        return user;
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        // 映射用户信息到LDAP上下文
        ctx.setAttributeValue("username", user.getUsername());
        ctx.setAttributeValue("email", extractEmail(ctx, user.getUsername()));
        ctx.setAttributeValue("telephoneNumber", extractPhoneNumber(ctx));
        ctx.setAttributeValue("description", "");
        ctx.setAttributeValue("userPassword", "");
        ctx.setAttributeValue("objectClass", new String[]{"top", "person", "organizationalPerson", "inetOrgPerson"});
        ctx.setAttributeValue("cn", user.getUsername());
        ctx.setAttributeValue("sn", user.getUsername());
        ctx.setAttributeValue("givenName", user.getUsername());
        ctx.setAttributeValue("displayName", user.getUsername());
        ctx.setAttributeValue("mail", extractEmail(ctx, user.getUsername()));
        ctx.setAttributeValue("telephoneNumber", extractPhoneNumber(ctx));
        ctx.setAttributeValue("jpegPhoto", "");

    }

    private String extractEmail(DirContextOperations ctx, String username) {
        String email = ctx.getStringAttribute("mail");
        if (email == null) {
            email = username + "@example.com"; // 默认邮箱
        }
        return email;
    }
    
    private String extractPhoneNumber(DirContextOperations ctx) {
        String phone = ctx.getStringAttribute("telephoneNumber");
        if (phone == null) {
            phone = ""; // 默认空电话号码
        }
        return phone;
    }
}
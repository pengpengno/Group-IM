package com.github.im.server.service;

import cn.hutool.core.util.RandomUtil;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.LdapDataEntry;
import org.springframework.ldap.core.LdapClient;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.Name;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.Attributes;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

/**
 * LDAP用户管理服务
 * 提供创建和管理LDAP用户的操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LdapUserService {

    private final LdapClient ldapClient;
    private final UserRepository userRepository;

    /**
     * 在LDAP中创建用户
     *
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param fullName 全名
     * @return 创建的用户对象
     */
    public User createLdapUser(String username, String password, String email, String fullName) {
        try {
            // 确保基础结构存在
            ensureBaseStructureExists();
            
            // 在LDAP中创建用户条目
            Name dn = LdapNameBuilder.newInstance()
                    .add("ou", "people")
                    .add("uid", username)
                    .build();

            Attributes attrs = new BasicAttributes();
            Attribute oc = new BasicAttribute("objectclass");
            oc.add("top");
            oc.add("person");
            oc.add("organizationalPerson");
            oc.add("inetOrgPerson");
            attrs.put(oc);
            attrs.put("uid", username);
            // 使用字节数组形式存储密码，确保正确编码
            log.debug("Setting user password for user {}: {}", username, password);
            attrs.put("userPassword", password.getBytes(StandardCharsets.UTF_8));
            attrs.put("cn", fullName);
            attrs.put("sn", username);
            attrs.put("mail", email);
            attrs.put("givenName", fullName);

            ldapClient.bind(dn).attributes(attrs).execute();

            log.info("Created LDAP user with DN: {}", dn);
        } catch (Exception e) {
            log.error("Failed to create LDAP user: {}", username, e);
            throw new RuntimeException("Failed to create LDAP user: " + username, e);
        }

        // 检查本地数据库中是否已存在该用户
        User user = userRepository.findByUsernameOrEmail(username).orElse(null);
        
        if (user == null) {
            // 如果本地数据库中不存在该用户，则创建一个新用户
            log.info("Creating new local user account for LDAP user: {}", username);
            
            user = User.builder()
                    .username(username)
                    .email(email)
                    .phoneNumber(RandomUtil.randomNumbers(11)) // LDAP用户可能没有电话号码
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

        return user;
    }

    /**
     * 确保基本的LDAP结构存在
     */
    private void ensureBaseStructureExists() {
        try {
            // 检查并创建OU=people
            try {
                Name peopleDN = LdapNameBuilder.newInstance()
                        .add("ou", "people")
                        .build();

                List<LdapDataEntry> entryList = ldapClient.search()
                        .query(query().base(peopleDN).where("objectClass").is("organizationalUnit"))
                        .toEntryList();
                log.debug("OU=people already exists ");
            } catch (Exception e) {
                // OU不存在，创建它
                log.info("OU=people does not exist, creating it...");
                createPeopleOU();
            }
        } catch (Exception e) {
            log.error("Error ensuring base LDAP structure exists", e);
        }
    }

    /**
     * 创建people组织单元
     */
    private void createPeopleOU() {
        try {
            Name peopleDN = LdapNameBuilder.newInstance()
                    .add("ou", "people")
                    .build();
            
            Attributes attrs = new BasicAttributes();
            Attribute oc = new BasicAttribute("objectclass");
            oc.add("top");
            oc.add("organizationalUnit");
            attrs.put(oc);
            attrs.put("ou", "people");
            
            ldapClient.bind(peopleDN).attributes(attrs).execute();
            log.info("Created OU=people");
        } catch (Exception ex) {
            log.error("Failed to create OU=people", ex);
            throw new RuntimeException("Failed to create OU=people", ex);
        }
    }

    /**
     * 删除LDAP用户
     *
     * @param username 用户名
     */
    public void deleteLdapUser(String username) {
        try {
            Name dn = LdapNameBuilder.newInstance()
                    .add("ou", "people")
                    .add("uid", username)
                    .build();
            
            ldapClient.unbind(dn).execute();
            
            log.info("Deleted LDAP user: {}", username);
        } catch (Exception e) {
            log.warn("Failed to delete LDAP user: {}", username, e);
            // 不抛出异常，因为即使删除失败也不应该影响整体流程
        }
    }
    
    /**
     * 检查LDAP用户是否存在
     * 
     * @param username 用户名
     * @return 如果用户存在返回true，否则返回false
     */
    public boolean isLdapUserExists(String username) {
        try {
            Name dn = LdapNameBuilder.newInstance()
                    .add("ou", "people")
                    .add("uid", username)
                    .build();
            
            var  list = ldapClient.search()
                .query(query().base(dn).where("objectClass").isPresent())
                .toEntryList();
            return true;
        } catch (org.springframework.ldap.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking if LDAP user exists: {}", username, e);
            return false;
        }
    }
    
    /**
     * 获取用户的完整DN
     * 
     * @param username 用户名
     * @return 用户的完整DN
     */
    public String getUserFullDn(String username) {
        Name dn = LdapNameBuilder.newInstance()
                .add("ou", "people")
                .add("uid", username)
                .build();
        return dn.toString();
    }
}
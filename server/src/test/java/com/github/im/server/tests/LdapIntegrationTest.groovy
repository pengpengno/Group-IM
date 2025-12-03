package com.github.im.server.tests

import com.github.im.server.model.User
import com.github.im.server.repository.UserRepository
import com.github.im.server.service.LdapUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.ldap.core.LdapClient
import org.springframework.ldap.core.DirContextOperations
import org.springframework.ldap.support.LdapNameBuilder
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import javax.naming.Name

import static org.springframework.ldap.query.LdapQueryBuilder.query

/**
 * LDAP集成测试类
 * 测试与真实LDAP服务器的交互
 */
@SpringBootTest
@ActiveProfiles("ldap")
class LdapIntegrationTest extends Specification {

    @Autowired
    LdapUserService ldapUserService

    @Autowired
    UserRepository userRepository

    @Autowired
    LdapClient ldapClient

    def "test create and delete LDAP user"() {
        given: "用户信息"
        String username = "integrationtestuser"
        String password = "testpassword123"
        String email = "integrationtestuser@example.com"
        String fullName = "Integration Test User"

        println("Creating LDAP user: $username")
        when: "创建LDAP用户"
        User user = ldapUserService.createLdapUser(username, password, email, fullName)

        then: "用户在本地数据库中创建成功"
        user != null
        user.username == username
        user.email == email

        and: "用户在LDAP中存在"
        Name dn = LdapNameBuilder.newInstance()
                .add("ou", "people")
                .add("uid", username)
                .build()
        
        isLdapUserExists(dn)
        
        when: "删除LDAP用户"
        ldapUserService.deleteLdapUser(username)

        then: "用户从LDAP中删除成功"
        // 验证用户确实已被删除
        isLdapUserDeleted(dn)

        cleanup: "清理测试数据"
        try {
            // 确保即使测试失败也能清理数据
            ldapUserService.deleteLdapUser(username)
        } catch (Exception e) {
            // 忽略删除异常
            println("Cleanup warning: ${e.message}")
        }
    }

    def "test create LDAP user that already exists in database"() {
        given: "已在数据库中存在的用户"
        String username = "existingdbuser"
        String email = "existingdbuser@example.com"
        
        and: "在数据库中创建用户"
        User existingUser = User.builder()
                .username(username)
                .email(email)
                .phoneNumber("123456789")
                .passwordHash("somehash")
                .status(true)
                .forcePasswordChange(false)
                .build()
        
        existingUser = userRepository.save(existingUser)
        
        and: "LDAP用户信息"
        String password = "testpassword123"
        String fullName = "Existing DB User"

        when: "尝试创建同名LDAP用户"
        User user = ldapUserService.createLdapUser(username, password, email, fullName)

        then: "返回已存在的用户"
        user.userId == existingUser.userId
        user.username == username
        user.email == email

        and: "LDAP中用户创建成功"
        Name dn = LdapNameBuilder.newInstance()
                .add("ou", "people")
                .add("uid", username)
                .build()
        
        isLdapUserExists(dn)

        cleanup: "清理测试数据"
        try {
            ldapUserService.deleteLdapUser(username)
        } catch (Exception e) {
            // 忽略删除异常
            println("Cleanup warning: ${e.message}")
        }
    }
    
    /**
     * 检查LDAP用户是否存在
     * @param dn 用户的DN
     * @return 如果用户存在则返回true，否则返回false
     */
    private boolean isLdapUserExists(Name dn) {
        try {
            ldapClient.search()
                .query(query().base(dn).where("objectClass").isPresent())
                .toList({ DirContextOperations ctx -> ctx })
            println("LDAP user found: $dn")
            return true
        } catch (org.springframework.ldap.NameNotFoundException e) {
            // 用户不存在
            println("LDAP user not found: $dn")
            return false
        } catch (Exception e) {
            // 其他异常
            println("Unexpected error when checking user existence: ${e.message}")
            return false
        }
    }
    
    /**
     * 检查LDAP用户是否已被删除
     * @param dn 用户的DN
     * @return 如果用户已被删除则返回true，否则返回false
     */
    private boolean isLdapUserDeleted(Name dn) {
        try {
            ldapClient.search()
                .query(query().base(dn).where("objectClass").isPresent())
                .toList({ DirContextOperations ctx -> ctx })
            // 如果能查找到用户，则说明未被删除
            println("LDAP user still exists: $dn")
            return false
        } catch (org.springframework.ldap.NameNotFoundException e) {
            // 如果抛出NameNotFoundException异常，则说明用户已被删除
            println("LDAP user successfully deleted: $dn")
            return true
        } catch (Exception e) {
            // 其他异常，打印日志并返回false
            println("Unexpected error when checking user deletion: ${e.message}")
            return false
        }
    }
}
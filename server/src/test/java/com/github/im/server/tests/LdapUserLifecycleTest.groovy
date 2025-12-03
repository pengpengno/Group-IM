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
 * LDAP用户生命周期测试类
 * 测试LDAP用户的完整生命周期：创建、查询、修改、删除
 */
@SpringBootTest
@ActiveProfiles("ldap")
class LdapUserLifecycleTest extends Specification {

    @Autowired
    LdapUserService ldapUserService

    @Autowired
    UserRepository userRepository

    @Autowired
    LdapClient ldapClient

    def "test complete LDAP user lifecycle"() {
        given: "用户信息"
        String username = "lifecycleuser"
        String password = "lifecyclepassword123"
        String email = "lifecycleuser@example.com"
        String fullName = "Lifecycle Test User"

        and: "确保用户不存在"
        deleteUserQuietly(username)

        when: "创建LDAP用户"
        User createdUser = ldapUserService.createLdapUser(username, password, email, fullName)

        then: "用户在本地数据库中创建成功"
        createdUser != null
        createdUser.username == username
        createdUser.email == email
        createdUser.userId != null

        and: "用户在LDAP中存在"
        isLdapUserExists(buildUserDN(username))

        and: "本地数据库中用户存在"
        def dbUserOptional = userRepository.findByUsernameOrEmail(username)
        assert dbUserOptional.isPresent()
        def dbUser = dbUserOptional.get()
        dbUser.username == username
        dbUser.email == email

        when: "再次查询用户（应返回已存在的用户）"
        User existingUser = ldapUserService.createLdapUser(username, password, email, fullName)

        then: "返回相同的用户"
        existingUser.userId == createdUser.userId
        existingUser.username == createdUser.username
        existingUser.email == createdUser.email

        when: "删除LDAP用户"
        ldapUserService.deleteLdapUser(username)

        then: "用户从LDAP中删除成功"
        isLdapUserDeleted(buildUserDN(username))

        and: "本地数据库中的用户仍然存在"
        userRepository.findByUsernameOrEmail(username).isPresent()

        cleanup: "清理测试数据"
        // 清理本地数据库中的用户（如果需要）
        try {
            def userToDelete = userRepository.findByUsernameOrEmail(username)
            if (userToDelete.isPresent()) {
                userRepository.delete(userToDelete.get())
            }
        } catch (Exception e) {
            println("Warning: Failed to delete user from database: ${e.message}")
        }
        
        deleteUserQuietly(username)
    }

    def "test LDAP user creation with minimal attributes"() {
        given: "最小用户信息"
        String username = "minimaluser"
        String password = "minimalpassword123"
        String email = "minimaluser@example.com"
        String fullName = "Minimal User"

        and: "确保用户不存在"
        deleteUserQuietly(username)

        when: "创建LDAP用户"
        User user = ldapUserService.createLdapUser(username, password, email, fullName)

        then: "用户创建成功"
        user != null
        user.username == username
        user.email == email

        and: "用户在LDAP中存在"
        isLdapUserExists(buildUserDN(username))

        cleanup: "清理测试数据"
        deleteUserQuietly(username)
    }

    /**
     * 静默删除用户（忽略异常）
     * @param username 用户名
     */
    private void deleteUserQuietly(String username) {
        try {
            ldapUserService.deleteLdapUser(username)
        } catch (Exception e) {
            // 忽略删除异常
            println("Warning: Failed to delete user $username: ${e.message}")
        }
    }

    /**
     * 构建用户DN
     * @param username 用户名
     * @return 用户的DN
     */
    private Name buildUserDN(String username) {
        return LdapNameBuilder.newInstance()
                .add("ou", "people")
                .add("uid", username)
                .build()
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
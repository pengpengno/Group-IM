package com.github.im.server.tests

import com.github.im.server.model.User
import com.github.im.server.repository.UserRepository
import com.github.im.server.service.LdapUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.ldap.core.LdapClient
import org.springframework.ldap.core.DirContextOperations
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import static org.springframework.ldap.query.LdapQueryBuilder.query

/**
 * LDAP用户搜索测试类
 * 测试在LDAP中搜索用户的功能
 */
@SpringBootTest
@ActiveProfiles("ldap")
class LdapUserSearchTest extends Specification {

    @Autowired
    LdapUserService ldapUserService

    @Autowired
    UserRepository userRepository

    @Autowired
    LdapClient ldapClient

    def "test search LDAP users by email"() {
        given: "创建测试用户"
        String username1 = "searchtestuser1"
        String username2 = "searchtestuser2"
        String password = "searchtestpassword123"
        String email1 = "searchtestuser1@example.com"
        String email2 = "searchtestuser2@example.com"
        String fullName1 = "Search Test User 1"
        String fullName2 = "Search Test User 2"

        and: "确保用户不存在"
        deleteUserQuietly(username1)
        deleteUserQuietly(username2)

        and: "创建LDAP用户"
        ldapUserService.createLdapUser(username1, password, email1, fullName1)
        ldapUserService.createLdapUser(username2, password, email2, fullName2)

        when: "通过邮件搜索用户"
        def results = ldapClient.search()
            .query(query().where("mail").is(email1))
            .toList({ DirContextOperations ctx -> ctx.getAttributes("") })

        then: "找到匹配的用户"
        results != null
        !results.isEmpty()
        results.size() == 1
        
        and: "用户属性正确"
        def attrs = results.get(0)
        attrs.get("uid").get() == username1
        attrs.get("mail").get() == email1

        cleanup: "清理测试数据"
        deleteUserQuietly(username1)
        deleteUserQuietly(username2)
    }

    def "test check if LDAP user exists using service method"() {
        given: "创建测试用户"
        String username = "existencetestuser"
        String password = "existencetestpassword123"
        String email = "existencetestuser@example.com"
        String fullName = "Existence Test User"

        and: "确保用户不存在"
        deleteUserQuietly(username)

        and: "验证用户不存在"
        !ldapUserService.isLdapUserExists(username)

        when: "创建LDAP用户"
        ldapUserService.createLdapUser(username, password, email, fullName)

        then: "用户存在"
        ldapUserService.isLdapUserExists(username)

        when: "删除LDAP用户"
        ldapUserService.deleteLdapUser(username)

        then: "用户不存在"
        !ldapUserService.isLdapUserExists(username)

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
}
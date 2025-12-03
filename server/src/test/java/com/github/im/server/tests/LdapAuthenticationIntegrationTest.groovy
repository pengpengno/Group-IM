package com.github.im.server.tests

import com.github.im.server.model.User
import com.github.im.server.repository.UserRepository
import com.github.im.server.service.AuthenticationService
import com.github.im.server.service.LdapUserService
import com.github.im.dto.user.LoginRequest
import org.springframework.LdapDataEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.ldap.NameNotFoundException
import org.springframework.ldap.core.LdapClient
import org.springframework.ldap.core.DirContextOperations
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.query.LdapQueryBuilder
import org.springframework.ldap.support.LdapNameBuilder
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import javax.naming.Name

import static org.springframework.ldap.query.LdapQueryBuilder.query

/**
 * LDAP认证集成测试类
 * 测试LDAP用户的创建、认证和登录流程
 */
@SpringBootTest
@ActiveProfiles("ldap")
class LdapAuthenticationIntegrationTest extends Specification {

    @Autowired
    LdapUserService ldapUserService

    @Autowired
    UserRepository userRepository

    @Autowired
    LdapClient ldapClient
    @Autowired
    LdapTemplate ldapTemplate

    @Autowired
    AuthenticationService authenticationService

    @Autowired
    AuthenticationManager authenticationManager

    def "test LDAP user creation and authentication"() {
        given: "LDAP用户信息"
        String username = "ldaptestuser"
        String password = "ldaptestpassword123"
        String email = "ldaptestuser@example.com"
        String fullName = "LDAP Test User"
        and: "确保用户不存在"

//        Optional<List<User>> userOpt = userRepository.findByNameOrEmail(username, Pageable.ofSize(1))
//        if (userOpt.isPresent()){
//            if (userOpt.get().size() > 1){
//                user = userOpt.get().get(0)
//                userRepository.delete(user);
//            }
//
//        }
        deleteUserQuietly(username)

        when: "创建LDAP用户"
        User user = ldapUserService.createLdapUser(username, password, email, fullName)

        then: "用户在本地数据库中创建成功"
        user != null
        user.username == username
        user.email == email

        and: "用户在LDAP中存在"
        isLdapUserExists(buildUserDN(username))

        when: "使用LDAP凭证进行认证"
        // 使用Spring Security的LDAP认证机制，而不是本地认证
        String filter = "(uid=" + username + ")";
        def authenticate = ldapTemplate.authenticate("ou=people", filter, password);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password)
        Authentication authentication = authenticationManager.authenticate(authToken)


        then: "认证成功"
        authentication != null
        authentication.isAuthenticated()
        authentication.getPrincipal() instanceof User
        ((User) authentication.getPrincipal()).username == username

        cleanup: "清理测试数据"
        deleteUserQuietly(username)
    }

    def "test LDAP user login via authentication service"() {
        given: "LDAP用户信息"
        String username = "ldaplogintestuser"
        String password = "ldaplogintestpassword123"
        String email = "ldaplogintestuser@example.com"
        String fullName = "LDAP Login Test User"

        and: "确保用户不存在"
        deleteUserQuietly(username)

        and: "创建LDAP用户"
        User user = ldapUserService.createLdapUser(username, password, email, fullName)

        and: "登录请求"
        LoginRequest loginRequest = new LoginRequest(username, password, null)

        when: "通过认证服务登录"
        def userInfoOptional = authenticationService.loginUser(loginRequest)

        then: "登录成功"
        userInfoOptional.isPresent()
        def userInfo = userInfoOptional.get()
        userInfo.username == username
        userInfo.email == email

        cleanup: "清理测试数据"
        deleteUserQuietly(username)
    }

    def "test LDAP user authentication with wrong password"() {
        given: "LDAP用户信息"
        String username = "wrongpwdtestuser"
        String password = "correctpassword123"
        String email = "wrongpwdtestuser@example.com"
        String fullName = "Wrong Password Test User"

        and: "错误密码"
        String wrongPassword = "wrongpassword123"

        and: "确保用户不存在"
        deleteUserQuietly(username)

        and: "创建LDAP用户"
        User user = ldapUserService.createLdapUser(username, password, email, fullName)

        when: "使用错误密码进行认证"
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, wrongPassword)
        authenticationManager.authenticate(authToken)

        then: "认证失败"
        thrown(Exception)

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
            def list = ldapClient.search()
                    .query(query().base(dn).where("objectClass").isPresent())
                    .toEntryList();
            println("LDAP user found: $dn , list : $list")
            return true
        } catch (NameNotFoundException ex) {
            // 用户不存在
            println("LDAP user not found: $dn,ex $ex.message")
            return false
        } catch (Exception e) {
            // 其他异常
            println("Unexpected error when checking user existence: ${e.message}")
            return false
        }
    }
}
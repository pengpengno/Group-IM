package com.github.im.server.controller

import com.github.im.dto.organization.CompanyDTO
import com.github.im.dto.user.LoginRequest
import com.github.im.dto.user.RegistrationRequest
import com.github.im.dto.user.UserBasicInfo
import com.github.im.dto.user.UserInfo
import com.github.im.server.model.User
import com.github.im.server.service.CompanyUserService
import com.github.im.server.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/**
 * UserController单元测试类
 * 使用Spock框架进行控制器层的行为驱动开发(BDD)测试
 */
class UserControllerTest extends Specification {

    // 被测试的控制器对象
    UserController userController

    // 依赖的服务Mock对象
    UserService userService = Mock()
    CompanyUserService companyUserService = Mock()

    def setup() {
        // 使用构造函数注入 Mock 服务，确保 Mock 正确生效
        userController = new UserController(userService, companyUserService)
    }

    def cleanup() {
        // 清理安全上下文
        SecurityContextHolder.clearContext()
    }

    // ==================== 用户注册接口测试 ====================

    def "测试用户注册接口成功场景"() {
        given: "准备有效的注册请求数据"
        def registrationRequest = new RegistrationRequest(
                "testuser",
                "test@example.com",
                "password123",
                "password123",
                "13800138000"
        )

        def userInfo = new UserInfo()
        userInfo.setUserId(1L)
        userInfo.setUsername("testuser")
        userInfo.setEmail("test@example.com")

        when: "调用用户注册接口"
        def response = userController.registerUser(registrationRequest)

        then: "验证响应结果"
        1 * userService.registerUser(registrationRequest) >> Optional.of(userInfo)
        
        response.getStatusCode() == HttpStatus.CREATED
        response.getBody().getUserId() == 1L
        response.getBody().getUsername() == "testuser"
    }

    def "测试用户注册接口失败场景"() {
        given: "准备无效的注册请求数据"
        def registrationRequest = new RegistrationRequest(
                "existinguser",
                "existing@example.com",
                "password123",
                "password123",
                "13800138000"
        )

        when: "调用用户注册接口"
        userController.registerUser(registrationRequest)

        then: "应该抛出异常"
        1 * userService.registerUser(registrationRequest) >> { throw new IllegalArgumentException("用户已存在！") }
        
        thrown(IllegalArgumentException)
    }

    // ==================== 获取公司列表接口测试 ====================

    def "测试获取当前用户公司列表成功场景"() {
        given: "准备当前用户和公司数据"
        def currentUser = new User()
        currentUser.setUserId(1L)
        currentUser.setUsername("testuser")

        def companies = [
                new CompanyDTO(1L, "Company A", "company_a"),
                new CompanyDTO(2L, "Company B", "company_b")
        ]

        when: "调用获取公司列表接口"
        def response = userController.getMyCompanies(currentUser)

        then: "验证响应结果"
        1 * companyUserService.getCompanyByUserId(1L) >> companies
        
        response.getStatusCode() == HttpStatus.OK
        response.getBody().getCode() == 200
        response.getBody().getMessage() == "获取公司列表成功"
        response.getBody().getData().size() == 2
        response.getBody().getData()[0].getName() == "Company A"
        response.getBody().getData()[1].getName() == "Company B"
    }

    def "测试获取当前用户公司列表异常场景"() {
        given: "准备当前用户数据"
        def currentUser = new User()
        currentUser.setUserId(1L)

        when: "调用获取公司列表接口，但服务抛出异常"
        def response = userController.getMyCompanies(currentUser)

        then: "验证异常处理结果"
        1 * companyUserService.getCompanyByUserId(1L) >> { throw new RuntimeException("数据库连接失败") }
        
        response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
        response.getBody().getCode() == 500
        response.getBody().getMessage().contains("获取用户公司列表失败")
    }

    // ==================== 用户查询接口测试 ====================

    def "测试根据用户名查询用户成功场景"() {
        given: "准备查询参数和预期结果"
        def username = "testuser"
        def userBasicInfo = new UserBasicInfo(1L, "testuser", "test@example.com", "13800138000")

        when: "调用根据用户名查询用户接口"
        def result = userController.getUserByUsername(username)

        then: "验证查询结果"
        1 * userService.findUserByUsername("testuser") >> Optional.of(userBasicInfo)
        
        result.isPresent()
        result.get().getUserId() == 1L
        result.get().getUsername() == "testuser"
    }

    def "测试根据用户名查询用户不存在场景"() {
        given: "准备查询参数"
        def username = "nonexistent"

        when: "调用根据用户名查询用户接口"
        def result = userController.getUserByUsername(username)

        then: "验证查询结果为空"
        1 * userService.findUserByUsername("nonexistent") >> Optional.empty()
        
        !result.isPresent()
    }

    def "测试根据用户ID查询用户成功场景"() {
        given: "准备查询参数和当前用户"
        def userId = 1L
        def currentUser = new User()
        currentUser.setUserId(2L)

        def userBasicInfo = new UserBasicInfo(1L, "targetuser", "target@example.com", "13800138000")

        when: "调用根据用户ID查询用户接口"
        def result = userController.getUserById(userId, currentUser)

        then: "验证查询结果"
        1 * userService.findUserByUserId(1L) >> Optional.of(userBasicInfo)
        
        result.isPresent()
        result.get().getUserId() == 1L
        result.get().getUsername() == "targetuser"
    }

    // ==================== 用户查询接口测试 ====================

    def "测试根据查询字符串查询用户成功场景"() {
        given: "准备查询参数和安全上下文"
        def query = "test"
        def authHeader = "Bearer token123"
        
        def currentUser = new User()
        currentUser.setUsername("currentuser")
        
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        
        def users = [
                new UserInfo(userId: 1L, username: "testuser1", email: "test1@example.com"),
                new UserInfo(userId: 2L, username: "testuser2", email: "test2@example.com")
        ]
        
        def page = new PageImpl<>(users, PageRequest.of(0, 100), 2)

        when: "调用根据查询字符串查询用户接口"
        def response = userController.queryUserByNameOrEmail(query, authHeader)

        then: "验证查询结果"
        1 * SecurityContextHolder.getContext() >> securityContext
        1 * securityContext.getAuthentication() >> authentication
        1 * authentication.getPrincipal() >> currentUser
        1 * userService.findUserByQueryStrings(query, authentication) >> page
        
        response.getStatusCode() == HttpStatus.OK
        response.getBody().getMetadata().getSize() == 2
    }

    // ==================== 用户登录接口测试 ====================

    def "测试用户登录接口成功场景"() {
        given: "准备登录请求数据"
        def loginRequest = new LoginRequest("testuser", "password123", null)
        def userInfo = new UserInfo(userId: 1L, username: "testuser")

        when: "调用用户登录接口"
        def response = userController.loginUser(loginRequest)

        then: "验证登录结果"
        1 * userService.loginUser(loginRequest) >> Optional.of(userInfo)
        
        response.getStatusCode() == HttpStatus.OK
        response.getBody().getUserId() == 1L
        response.getBody().getUsername() == "testuser"
    }

    def "测试用户登录接口失败场景"() {
        given: "准备无效的登录请求数据"
        def loginRequest = new LoginRequest("invaliduser", "wrongpassword", null)

        when: "调用用户登录接口"
        def response = userController.loginUser(loginRequest)

        then: "验证登录失败结果"
        1 * userService.loginUser(loginRequest) >> Optional.empty()
        
        response.getStatusCode() == HttpStatus.UNAUTHORIZED
    }

    def "测试用户登录接口空请求场景"() {
        given: "准备null的登录请求"
        def loginRequest = null

        when: "调用用户登录接口"
        userController.loginUser(loginRequest)

        then: "应该抛出验证异常"
        thrown(IllegalArgumentException)
    }

    // ==================== 密码重置接口测试 ====================

    def "测试密码重置接口成功场景"() {
        given: "准备密码重置参数"
        def userId = 1L
        def newPassword = "newpassword123"
        def updatedUser = new User()
        updatedUser.setUserId(1L)
        updatedUser.setUsername("testuser")

        when: "调用密码重置接口"
        def result = userController.resetPassword(userId, newPassword)

        then: "验证密码重置结果"
        1 * userService.resetPassword(1L, "newpassword123") >> updatedUser
        
        result.getUserId() == 1L
        result.getUsername() == "testuser"
    }

    // ==================== 异常处理测试 ====================

    def "测试全局异常处理"() {
        given: "准备会导致异常的请求"
        def registrationRequest = new RegistrationRequest("", "", "", "", "")

        when: "调用可能抛出异常的接口"
        userController.registerUser(registrationRequest)

        then: "验证异常被正确处理"
        1 * userService.registerUser(registrationRequest) >> { 
            throw new IllegalArgumentException("用户名和邮箱不能为空") 
        }
        
        thrown(IllegalArgumentException)
    }

    def "测试边界条件测试"() {
        given: "准备超长用户名查询"
        def longUsername = "a" * 1000
        def userBasicInfo = new UserBasicInfo(1L, longUsername, "test@example.com", null)

        when: "调用查询接口"
        def result = userController.getUserByUsername(longUsername)

        then: "验证处理结果"
        1 * userService.findUserByUsername(longUsername) >> Optional.of(userBasicInfo)
        
        result.isPresent()
        result.get().getUsername().length() == 1000
    }
}
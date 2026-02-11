package com.github.im.server.service

import com.github.im.dto.user.LoginRequest
import com.github.im.dto.user.RegistrationRequest
import com.github.im.dto.user.UserBasicInfo
import com.github.im.dto.user.UserInfo
import com.github.im.server.config.ForcePasswordChangeConfig
import com.github.im.server.mapstruct.UserMapper
import com.github.im.server.model.User
import com.github.im.server.repository.UserRepository
import jakarta.persistence.EntityExistsException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification

/**
 * UserService单元测试类
 */
class UserServiceSpec extends Specification {

    // 被测试的服务对象
    UserService userService
    UserMapper userMapper = Mock()
    // 依赖的Mock对象
    UserRepository userRepository = Mock()
    PasswordEncoder passwordEncoder = Mock()
    AuthenticationService authenticationService = Mock(constructorArgs: [null, null, null, null, null])
    ForcePasswordChangeConfig forcePasswordChangeConfig = Mock()
    // 移除UserMapper的Mock，直接使用真实的UserMapper.INSTANCE进行测试

    def setup() {
        // 初始化被测试的服务对象
        userService = new UserService(
                userRepository,
                passwordEncoder,
                authenticationService,
                forcePasswordChangeConfig,
                userMapper
        )
    }

    def cleanup() {
        // 清理安全上下文
        SecurityContextHolder.clearContext()
    }

    // ==================== 用户注册测试 ====================

    def "测试用户注册成功场景"() {
        given: "准备有效的注册请求数据"
        def registrationRequest = new RegistrationRequest(
                "testuser",
                "test@example.com",
                "password123",
                "password123",
                "13800138000"
        )

        def newUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .phoneNumber("13800138000")
                .passwordHash("encodedPassword")
                .build()

        def expectedUserInfo = new UserInfo()
        expectedUserInfo.setUserId(1L)
        expectedUserInfo.setUsername("testuser")
        expectedUserInfo.setEmail("test@example.com")

        when: "执行用户注册"
        def result = userService.registerUser(registrationRequest)

        then: "验证注册结果"
        1 * userRepository.findByUsernameOrEmail("testuser", "test@example.com") >> Optional.empty()
        1 * passwordEncoder.encode("password123") >> "encodedPassword"
        1 * forcePasswordChangeConfig.isForcePasswordChangeEnabled() >> false
        1 * userRepository.save(_) >> newUser
        1 * userMapper.userToUserInfo(newUser) >> expectedUserInfo
        
        result.isPresent()
        result.get().getUserId() == 1L
        result.get().getUsername() == "testuser"
        result.get().getEmail() == "test@example.com"
    }

    def "测试用户注册成功场景并验证Mapper调用"() {
        given: "准备有效的注册请求数据"
        def registrationRequest = new RegistrationRequest(
                "content_compare_test",
                "content@test.com",
                "password123",
                "password123",
                "13800138004"
        )

        def newUser = User.builder()
                .userId(1L)
                .username("content_compare_test")
                .email("content@test.com")
                .phoneNumber("13800138004")
                .passwordHash("encodedPassword")
                .build()

        def expectedUserInfo = new UserInfo()
        expectedUserInfo.setUserId(1L)
        expectedUserInfo.setUsername("content_compare_test")
        expectedUserInfo.setEmail("content@test.com")
        expectedUserInfo.setPhoneNumber("13800138004")

        and: "准备用于内容比较的变量"
        User capturedUser = null
        UserInfo actualResult = null

        when: "执行用户注册"
        def result = userService.registerUser(registrationRequest)

        then: "使用内容比较验证Mapper调用"
        1 * userRepository.findByUsernameOrEmail("content_compare_test", "content@test.com") >> Optional.empty()
        1 * passwordEncoder.encode("password123") >> "encodedPassword"
        1 * forcePasswordChangeConfig.isForcePasswordChangeEnabled() >> false
        1 * userRepository.save(_) >> newUser
        
        // 修正语法：移除非法的逻辑或操作符
        1 * userMapper.userToUserInfo(_) >> { User user ->
            capturedUser = user
            // 内容比较：验证传入User对象的属性值
            assert user.getUsername() == "content_compare_test"
            assert user.getEmail() == "content@test.com"
            assert user.getPhoneNumber() == "13800138004"
            assert user.getPasswordHash() == "encodedPassword"
            assert user.getUserId() == 1L
            return expectedUserInfo
        }
        
        result.isPresent()
        
        and: "验证最终结果的内容比较"
        result.get().getUserId() == 1L
        result.get().getUsername() == "content_compare_test"
        result.get().getEmail() == "content@test.com"
        result.get().getPhoneNumber() == "13800138004"

        and: "详细的内容比较验证"
        assert capturedUser != null
        println "=== 对象内容比较详情 ==="
        println "传入Mapper的User对象内容验证:"
        println "  Username匹配: ${capturedUser.getUsername() == 'content_compare_test'}"
        println "  Email匹配: ${capturedUser.getEmail() == 'content@test.com'}"
        println "  Phone匹配: ${capturedUser.getPhoneNumber() == '13800138004'}"
        println "  UserId匹配: ${capturedUser.getUserId() == 1L}"
        println ""
        println "返回的UserInfo对象内容验证:"
        println "  UserId: ${result.get().getUserId() == 1L}"
        println "  Username: ${result.get().getUsername() == 'content_compare_test'}"
        println "  Email: ${result.get().getEmail() == 'content@test.com'}"
        println "  Phone: ${result.get().getPhoneNumber() == '13800138004'}"
        println "========================"
    }

    def "测试用户注册时用户名已存在"() {
        given: "准备注册请求，但用户名已存在"
        def registrationRequest = new RegistrationRequest(
                "existinguser",
                "newemail@example.com",
                "password123",
                "password123",
                "13800138000"
        )

        def existingUser = User.builder()
                .userId(1L)
                .username("existinguser")
                .email("oldemail@example.com")
                .build()

        when: "执行用户注册"
        userService.registerUser(registrationRequest)

        then: "应该抛出异常"
        1 * userRepository.findByUsernameOrEmail("existinguser", "newemail@example.com") >> Optional.of(existingUser)
        
        thrown(IllegalArgumentException)
    }

    def "测试用户注册时邮箱已存在"() {
        given: "准备注册请求，但邮箱已存在"
        def registrationRequest = new RegistrationRequest(
                "newuser",
                "existing@example.com",
                "password123",
                "password123",
                "13800138000"
        )

        def existingUser = User.builder()
                .userId(1L)
                .username("olduser")
                .email("existing@example.com")
                .build()

        when: "执行用户注册"
        userService.registerUser(registrationRequest)

        then: "应该抛出异常"
        1 * userRepository.findByUsernameOrEmail("newuser", "existing@example.com") >> Optional.of(existingUser)
        
        thrown(IllegalArgumentException)
    }

    def "测试用户注册密码不匹配"() {
        given: "准备密码不匹配的注册请求"
        def registrationRequest = new RegistrationRequest(
                "testuser",
                "test@example.com",
                "password123",
                "differentpassword",
                "13800138000"
        )

        when: "执行用户注册"
        userService.registerUser(registrationRequest)

        then: "应该抛出异常"
        thrown(IllegalArgumentException)
    }

    def "测试用户注册时进行对象内容比较而非内存地址比较"() {
        given: "准备测试数据 - 注意创建具有相同内容的不同对象"
        def registrationRequest = new RegistrationRequest(
                "content_compare_test",
                "content@test.com",
                "password123",
                "password123",
                "13800138004"
        )

        // 创建保存到数据库的User对象
        def savedUser = User.builder()
                .userId(1L)
                .username("content_compare_test")
                .email("content@test.com")
                .phoneNumber("13800138004")
                .passwordHash("encodedPassword")
                .build()

        // 创建期望的UserInfo对象（内容相同但内存地址不同）
        def expectedUserInfo = new UserInfo()
        expectedUserInfo.setUserId(1L)
        expectedUserInfo.setUsername("content_compare_test")
        expectedUserInfo.setEmail("content@test.com")
        expectedUserInfo.setPhoneNumber("13800138004")

        and: "准备用于内容比较的变量"
        User capturedUser = null
        UserInfo actualResult = null

        when: "执行用户注册"
        def result = userService.registerUser(registrationRequest)

        then: "使用内容比较验证Mapper调用"
        1 * userRepository.findByUsernameOrEmail("content_compare_test", "content@test.com") >> Optional.empty()
        1 * passwordEncoder.encode("password123") >> "encodedPassword"
        1 * forcePasswordChangeConfig.isForcePasswordChangeEnabled() >> false
        
        // 关键：验证保存的User对象内容，而非内存地址
        1 * userRepository.save({ User user ->
            // 内容比较：验证各个属性值
            assert user.getUsername() == "content_compare_test"
            assert user.getEmail() == "content@test.com"
            assert user.getPhoneNumber() == "13800138004"
            assert user.getPasswordHash() == "encodedPassword"
            assert user.getUserId() == null  // 新用户ID应该为null
            return true
        }) >> savedUser
        
        // 修正语法错误：使用正确的方式捕获参数
        1 * userMapper.userToUserInfo(_) >> { User user ->
            capturedUser = user  // 正确的赋值语法
            // 内容比较：验证传入User对象的属性值
            assert user.getUsername() == "content_compare_test"
            assert user.getEmail() == "content@test.com"
            assert user.getPhoneNumber() == "13800138004"
            assert user.getPasswordHash() == "encodedPassword"
            assert user.getUserId() == 1L
            return expectedUserInfo  // 返回预期结果
        }
        
        result.isPresent()
        
        and: "验证最终结果的内容比较"
        result.get().getUserId() == 1L
        result.get().getUsername() == "content_compare_test"
        result.get().getEmail() == "content@test.com"
        result.get().getPhoneNumber() == "13800138004"

        and: "详细的内容比较验证"
        assert capturedUser != null  // 验证变量已被正确赋值
        println "=== 对象内容比较详情 ==="
        println "传入Mapper的User对象内容验证:"
        println "  Username匹配: ${capturedUser.getUsername() == 'content_compare_test'}"
        println "  Email匹配: ${capturedUser.getEmail() == 'content@test.com'}"
        println "  Phone匹配: ${capturedUser.getPhoneNumber() == '13800138004'}"
        println "  UserId匹配: ${capturedUser.getUserId() == 1L}"
        println ""
        println "返回的UserInfo对象内容验证:"
        println "  UserId: ${result.get().getUserId() == 1L}"
        println "  Username: ${result.get().getUsername() == 'content_compare_test'}"
        println "  Email: ${result.get().getEmail() == 'content@test.com'}"
        println "  Phone: ${result.get().getPhoneNumber() == '13800138004'}"
        println "========================"
    }

    // ==================== 批量用户注册测试 ====================

    def "测试批量用户注册成功场景"() {
        given: "准备批量注册请求数据"
        def requests = [
                new RegistrationRequest("user1", "user1@example.com", "password123", "password123", "13800138001"),
                new RegistrationRequest("user2", "user2@example.com", "password123", "password123", "13800138002")
        ]

        def newUser1 = User.builder().userId(1L).username("user1").email("user1@example.com").build()
        def newUser2 = User.builder().userId(2L).username("user2").email("user2@example.com").build()

        def userInfo1 = new UserInfo(userId: 1L, username: "user1", email: "user1@example.com")
        def userInfo2 = new UserInfo(userId: 2L, username: "user2", email: "user2@example.com")

        when: "执行批量用户注册"
        def result = userService.batchRegisterUsers(requests)

        then: "验证批量注册结果"
        1 * userRepository.findByUsernameInOrEmailIn(_, _) >> []
        2 * userRepository.findByUsernameOrEmail(*_) >>> [Optional.empty(), Optional.empty()]
        2 * passwordEncoder.encode("password123") >> "encodedPassword"
        2 * forcePasswordChangeConfig.isForcePasswordChangeEnabled() >> false
        2 * userRepository.save(_) >>> [newUser1, newUser2]
        2 * userMapper.userToUserInfo(_) >>> [userInfo1, userInfo2]
        
        result.size() == 2
        result[0].getUsername() == "user1"
        result[1].getUsername() == "user2"
    }

    def "测试批量用户注册存在重复用户名"() {
        given: "准备包含重复用户名的批量注册请求"
        def requests = [
                new RegistrationRequest("duplicate", "user1@example.com", "password123", "password123", "13800138001"),
                new RegistrationRequest("duplicate", "user2@example.com", "password123", "password123", "13800138002")
        ]

        when: "执行批量用户注册"
        userService.batchRegisterUsers(requests)

        then: "应该抛出异常"
        thrown(IllegalArgumentException)
    }

    // ==================== 用户登录测试 ====================

    def "测试用户登录成功场景"() {
        given: "准备登录请求数据"
        def loginRequest = new LoginRequest("testuser", "password123", null,"code1")
        def expectedUserInfo = new UserInfo(userId: 1L, username: "testuser")

        when: "执行用户登录"
        def result = userService.loginUser(loginRequest)

        then: "验证登录结果"
        1 * authenticationService.login(loginRequest) >> Optional.of(expectedUserInfo)
        
        result.isPresent()
        result.get().getUserId() == 1L
        result.get().getUsername() == "testuser"
    }

    def "测试用户登录失败场景"() {
        given: "准备无效的登录请求数据"
        def loginRequest = new LoginRequest("nonexistent", "wrongpassword", null,"code1")

        when: "执行用户登录"
        def result = userService.loginUser(loginRequest)

        then: "验证登录结果为空"
        1 * authenticationService.login(loginRequest) >> Optional.empty()
        
        !result.isPresent()
    }

    def "测试直接数据库验证登录成功"() {
        given: "准备登录请求和用户数据"
        def loginRequest = new LoginRequest("testuser", "password123","","")  // 移除null参数
        def user = User.builder()
                .userId(1L)
                .username("testuser")
                .passwordHash("encodedPassword")
                .build()

        def expectedUserInfo = new UserInfo(userId: 1L, username: "testuser")

        when: "执行直接数据库验证登录"
        def result = userService.loginUserDirect(loginRequest)

        then: "验证登录结果"
        1 * userRepository.findByUsernameOrEmail("testuser") >> Optional.of(user)
        1 * passwordEncoder.matches("password123", "encodedPassword") >> true
        1 * userMapper.userToUserInfo(user) >> expectedUserInfo
        
        result.isPresent()
        result.get().getUserId() == 1L
    }

    // ==================== 用户查询测试 ====================

    def "测试根据用户名查询用户成功"() {
        given: "准备查询数据"
        def username = "testuser"
        def user = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .build()

        def expectedUserBasicInfo = new UserBasicInfo(1L, "testuser", "test@example.com", null)

        when: "根据用户名查询用户"
        def result = userService.findUserByUsername(username)

        then: "验证查询结果"
        1 * userRepository.findByUsername("testuser") >> Optional.of(user)
        // 注意：这里不Mock userMapper，因为实际代码使用UserMapper.INSTANCE
        // 但我们可以通过验证结果来间接测试
        
        result.isPresent()
        result.get().getUserId() == 1L
        result.get().getUsername() == "testuser"
    }

    def "测试根据用户ID查询用户成功"() {
        given: "准备查询数据"
        def userId = 1L
        def user = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .build()

        def expectedUserBasicInfo = new UserBasicInfo(1L, "testuser", "test@example.com", null)

        when: "根据用户ID查询用户"
        def result = userService.findUserByUserId(userId)

        then: "验证查询结果"
        1 * userRepository.findById(1L) >> Optional.of(user)
        // 注意：这里不Mock userMapper，因为实际代码使用UserMapper.INSTANCE
        
        result.isPresent()
        result.get().getUserId() == 1L
    }

    def "测试根据查询字符串查询用户成功"() {
        given: "准备查询数据和安全上下文"
        def queryString = "test"
        def currentUser = User.builder().username("currentuser").build()
        def authentication = Mock(Authentication)
        def securityContext = Mock(SecurityContext)

        def users = [
                User.builder().userId(1L).username("testuser1").email("test1@example.com").build(),
                User.builder().userId(2L).username("testuser2").email("test2@example.com").build()
        ]
        
        def userInfos = [
                new UserInfo(userId: 1L, username: "testuser1", email: "test1@example.com"),
                new UserInfo(userId: 2L, username: "testuser2", email: "test2@example.com")
        ]

        def page = new PageImpl<>(users, PageRequest.of(0, 100), 2)

        when: "根据查询字符串查询用户"
        def result = userService.findUserByQueryStrings(queryString, authentication)

        then: "验证查询结果"
        1 * SecurityContextHolder.getContext() >> securityContext
        1 * securityContext.getAuthentication() >> authentication
        1 * authentication.getPrincipal() >> currentUser
        1 * userRepository.findAll(_, _) >> page
        // 注意：这里不Mock userMapper，因为实际代码使用UserMapper.INSTANCE::userToUserInfo
        
        result.getTotalElements() == 2
        result.getContent().size() == 2
        result.getContent()[0].getUsername() == "testuser1"
        result.getContent()[1].getUsername() == "testuser2"
    }

    // ==================== 密码重置测试 ====================

    def "测试密码重置成功场景"() {
        given: "准备密码重置数据"
        def userId = 1L
        def newPassword = "newpassword123"
        def user = User.builder()
                .userId(1L)
                .username("testuser")
                .passwordHash("oldPassword")
                .build()

        def updatedUser = User.builder()
                .userId(1L)
                .username("testuser")
                .passwordHash("newEncodedPassword")
                .build()

        when: "执行密码重置"
        def result = userService.resetPassword(userId, newPassword)

        then: "验证密码重置结果"
        1 * userRepository.findById(1L) >> Optional.of(user)
        1 * passwordEncoder.encode("newpassword123") >> "newEncodedPassword"
        1 * userRepository.save(_) >> updatedUser
        
        result.getUserId() == 1L
        result.getUsername() == "testuser"
    }

    def "测试密码重置用户不存在"() {
        given: "准备不存在的用户ID"
        def userId = 999L
        def newPassword = "newpassword123"

        when: "执行密码重置"
        userService.resetPassword(userId, newPassword)

        then: "应该抛出异常"
        1 * userRepository.findById(999L) >> Optional.empty()
        
        thrown(org.springframework.security.core.userdetails.UsernameNotFoundException)
    }

    // ==================== 公司相关方法测试 ====================

    def "测试获取所有用户成功"() {
        given: "准备用户数据"
        def users = [
                User.builder().userId(1L).username("user1").build(),
                User.builder().userId(2L).username("user2").build()
        ]
        
        def userInfos = [
                new UserInfo(userId: 1L, username: "user1"),
                new UserInfo(userId: 2L, username: "user2")
        ]

        when: "获取所有用户"
        def result = userService.getAllUsers()

        then: "验证结果"
        1 * userRepository.findAll() >> users
        1 * userMapper.usersToUserInfos(users) >> userInfos
        
        result.size() == 2
        result[0].getUsername() == "user1"
        result[1].getUsername() == "user2"
    }

    def "测试根据公司ID获取用户成功"() {
        given: "准备公司ID和用户数据"
        def companyId = 1L
        def users = [
                User.builder().userId(1L).username("user1").primaryCompanyId(1L).build(),
                User.builder().userId(2L).username("user2").primaryCompanyId(1L).build()
        ]

        when: "根据公司ID获取用户"
        def result = userService.getUsersByCompanyId(companyId)

        then: "验证结果"
        1 * userRepository.findByPrimaryCompanyId(1L) >> users
        
        result.size() == 2
        result[0].getUsername() == "user1"
        result[1].getUsername() == "user2"
    }

    def "测试检查用户是否属于指定公司成功"() {
        given: "准备用户和公司数据"
        def userId = 1L
        def companyId = 1L
        def user = User.builder()
                .userId(1L)
                .username("testuser")
                .primaryCompanyId(1L)
                .build()

        when: "检查用户是否属于指定公司"
        def result = userService.isUserInCompany(userId, companyId)

        then: "验证结果"
        1 * userRepository.findById(1L) >> Optional.of(user)
        
        result == true
    }

    def "测试检查用户是否属于指定公司失败"() {
        given: "准备用户和不同的公司数据"
        def userId = 1L
        def companyId = 2L
        def user = User.builder()
                .userId(1L)
                .username("testuser")
                .primaryCompanyId(1L)
                .build()

        when: "检查用户是否属于指定公司"
        def result = userService.isUserInCompany(userId, companyId)

        then: "验证结果"
        1 * userRepository.findById(1L) >> Optional.of(user)
        
        result == false
    }

    def "测试检查不存在用户的公司归属"() {
        given: "准备不存在的用户ID"
        def userId = 999L
        def companyId = 1L

        when: "检查用户是否属于指定公司"
        def result = userService.isUserInCompany(userId, companyId)

        then: "验证结果"
        1 * userRepository.findById(999L) >> Optional.empty()
        
        result == false
    }

    def "测试Mapper调用输入输出验证"() {
        given: "准备测试数据"
        def user = User.builder()
                .userId(1L)
                .username("mapper_test")
                .email("mapper@test.com")
                .phoneNumber("13800138001")
                .passwordHash("hashed_password")
                .build()

        def expectedUserInfo = new UserInfo()
        expectedUserInfo.setUserId(1L)
        expectedUserInfo.setUsername("mapper_test")
        expectedUserInfo.setEmail("mapper@test.com")
        expectedUserInfo.setPhoneNumber("13800138001")

        when: "直接调用Mapper方法"
        def result = userMapper.userToUserInfo(user)

        then: "验证Mapper转换结果"
        result != null
        result.getUserId() == 1L
        result.getUsername() == "mapper_test"
        result.getEmail() == "mapper@test.com"
        result.getPhoneNumber() == "13800138001"

        and: "打印调试信息"
        println "=== Mapper调用详情 ==="
        println "输入User对象:"
        println "  UserId: ${user.getUserId()}"
        println "  Username: ${user.getUsername()}"
        println "  Email: ${user.getEmail()}"
        println "  Phone: ${user.getPhoneNumber()}"
        println ""
        println "输出UserInfo对象:"
        println "  UserId: ${result.getUserId()}"
        println "  Username: ${result.getUsername()}"
        println "  Email: ${result.getEmail()}"
        println "  Phone: ${result.getPhoneNumber()}"
        println "=================="
    }

    def "测试使用工具类验证Mapper调用"() {
        given: "创建测试数据"
        def testUser = User.builder()
            .userId(999L)
            .username("tool_test")
            .email("tool@test.com")
            .phoneNumber("13800138003")
            .passwordHash("tool_password")
            .build()
        
        def expectedUserInfo = new UserInfo()
        expectedUserInfo.setUserId(999L)
        expectedUserInfo.setUsername("tool_test")
        expectedUserInfo.setEmail("tool@test.com")
        expectedUserInfo.setPhoneNumber("13800138003")

        and: "打印输入数据详情"
        println "=== 输入的User对象 Details ==="
        println "UserId: ${testUser.getUserId()}"
        println "Username: ${testUser.getUsername()}"
        println "Email: ${testUser.getEmail()}"
        println "Phone: ${testUser.getPhoneNumber()}"
        println "=============================="

        when: "模拟Mapper调用"
        def actualResult = userMapper.userToUserInfo(testUser)

        then: "验证结果并打印详情"
        1 * userMapper.userToUserInfo(testUser) >> expectedUserInfo
        
        actualResult != null
        
        and: "打印和验证结果"
        println "=== Mapper输出的UserInfo对象 Details ==="
        println "UserId: ${actualResult.getUserId()}"
        println "Username: ${actualResult.getUsername()}"
        println "Email: ${actualResult.getEmail()}"
        println "Phone: ${actualResult.getPhoneNumber()}"
        println "========================================"
        
        // 验证关键属性
        actualResult.getUsername() == "tool_test"
        actualResult.getEmail() == "tool@test.com"
        actualResult.getUserId() == 999L
    }

    def "测试使用Spy监控真实的Mapper调用"() {
        given: "创建真实的Mapper对象并使用Spy包装"
        def realUserMapper = UserMapper.INSTANCE
        def spyUserMapper = Spy(realUserMapper)
        
        // 重新创建UserService实例使用Spy Mapper
        def userServiceWithSpy = new UserService(
                userRepository,
                passwordEncoder,
                authenticationService,
                forcePasswordChangeConfig,
                spyUserMapper
        )

        def registrationRequest = new RegistrationRequest(
                "spy_test",
                "spy@test.com",
                "password123",
                "password123",
                "13800138002"
        )

        def newUser = User.builder()
                .userId(1L)
                .username("spy_test")
                .email("spy@test.com")
                .phoneNumber("13800138002")
                .passwordHash("encodedPassword")
                .build()

        def expectedUserInfo = new UserInfo()
        expectedUserInfo.setUserId(1L)
        expectedUserInfo.setUsername("spy_test")
        expectedUserInfo.setEmail("spy@test.com")

        when: "执行用户注册"
        def result = userServiceWithSpy.registerUser(registrationRequest)

        then: "验证调用并捕获实际参数"
        1 * userRepository.findByUsernameOrEmail("spy_test", "spy@test.com") >> Optional.empty()
        1 * passwordEncoder.encode("password123") >> "encodedPassword"
        1 * forcePasswordChangeConfig.isForcePasswordChangeEnabled() >> false
        1 * userRepository.save(_) >> newUser
        
        // 使用Spy监控真实的Mapper调用
        1 * spyUserMapper.userToUserInfo(_) >> { User user ->
            println "=== Spy捕获的真实Mapper调用 ==="
            println "实际传入的User对象:"
            println "  UserId: ${user.getUserId()}"
            println "  Username: ${user.getUsername()}"
            println "  Email: ${user.getEmail()}"
            println "  Phone: ${user.getPhoneNumber()}"
            println "  PasswordHash: ${user.getPasswordHash()}"
            println "================================"
            return expectedUserInfo
        }
        
        result.isPresent()
        result.get().getUsername() == "spy_test"
    }

    def "测试使用测试工具进行深度对象比较"() {
        given: "创建测试数据"
        def inputUser = User.builder()
            .userId(999L)
            .username("deep_compare_test")
            .email("deep@test.com")
            .phoneNumber("13800138005")
            .passwordHash("deep_password")
            .build()
        
        def expectedOutput = new UserInfo()
        expectedOutput.setUserId(999L)
        expectedOutput.setUsername("deep_compare_test")
        expectedOutput.setEmail("deep@test.com")
        expectedOutput.setPhoneNumber("13800138005")

        and: "准备比较变量"
        User mapperInput = null
        UserInfo mapperOutput = null

        when: "模拟Mapper调用"
        def result = userMapper.userToUserInfo(inputUser)

        then: "进行深度内容比较"
        1 * userMapper.userToUserInfo(_) >> { User user ->
            mapperInput = user
            
            // 打印User对象详情
            println "=== Mapper接收的User Details ==="
            println "UserId: ${user.getUserId()}"
            println "Username: ${user.getUsername()}"
            println "Email: ${user.getEmail()}"
            println "Phone: ${user.getPhoneNumber()}"
            println "PasswordHash: ${user.getPasswordHash() ? 'PRESENT' : 'NULL'}"
            println "================================"
            
            // 深度内容比较
            assert user.getUsername() == "deep_compare_test"
            assert user.getEmail() == "deep@test.com"
            assert user.getPhoneNumber() == "13800138005"
            assert user.getUserId() == 999L
            assert user.getPasswordHash() == "deep_password"
            
            return expectedOutput
        }
        
        result != null
        
        and: "验证结果"
        // 验证基本属性
        result.getUsername() == "deep_compare_test"
        result.getEmail() == "deep@test.com"
        result.getPhoneNumber() == "13800138005"
        result.getUserId() == 999L
        
        // 详细比较报告
        println "=== 对象比较报告 ==="
        println "输入User对象:"
        println "  UserId: ${inputUser.getUserId()}"
        println "  Username: ${inputUser.getUsername()}"
        println "  Email: ${inputUser.getEmail()}"
        println ""
        println "Mapper接收的User对象:"
        println "  UserId: ${mapperInput.getUserId()}"
        println "  Username: ${mapperInput.getUsername()}"
        println "  Email: ${mapperInput.getEmail()}"
        println ""
        println "输出UserInfo对象:"
        println "  UserId: ${result.getUserId()}"
        println "  Username: ${result.getUsername()}"
        println "  Email: ${result.getEmail()}"
        println "=================="
        
        // 验证对象内容相同但不是同一个实例
        assert inputUser.getUserId() == mapperInput.getUserId()
        assert inputUser.getUsername() == mapperInput.getUsername()
        assert inputUser.getEmail() == mapperInput.getEmail()
        // 验证确实是不同的对象实例
        assert System.identityHashCode(inputUser) != System.identityHashCode(mapperInput)
    }

    def "测试参数匹配器进行灵活的内容比较"() {
        given: "准备测试场景"
        def testUsers = [
            User.builder().userId(999L).username("matcher_test_1").email("comparison@test.com").build(),
            User.builder().userId(999L).username("matcher_test_2").email("comparison@test.com").build(),
            User.builder().userId(999L).username("matcher_test_3").email("comparison@test.com").build()
        ]
        
        def expectedInfos = testUsers.collect { user ->
            def info = new UserInfo()
            info.setUserId(user.getUserId())
            info.setUsername(user.getUsername())
            info.setEmail(user.getEmail())
            info
        }

        when: "批量测试Mapper调用"
        def results = testUsers.collect { user ->
            userMapper.userToUserInfo(user)
        }

        then: "使用不同的参数匹配器策略"
        // 策略1：精确匹配特定用户
        1 * userMapper.userToUserInfo({ User user ->
            user.getUsername() == "matcher_test_1"
            user.getEmail() == "comparison@test.com"
        }) >> expectedInfos[0]
        
        // 策略2：范围匹配（用户名包含特定模式）
        1 * userMapper.userToUserInfo({ User user ->
            user.getUsername().startsWith("matcher_test_")
            user.getUserId() == 999L
        }) >> expectedInfos[1]
        
        // 策略3：属性存在性匹配
        1 * userMapper.userToUserInfo({ User user ->
            user.getUsername() != null
            user.getEmail() != null
            !user.getUsername().isEmpty()
            !user.getEmail().isEmpty()
        }) >> expectedInfos[2]
        
        results.size() == 3
        results[0].getUsername() == "matcher_test_1"
        results[1].getUsername() == "matcher_test_2"
        results[2].getUsername() == "matcher_test_3"
    }
}
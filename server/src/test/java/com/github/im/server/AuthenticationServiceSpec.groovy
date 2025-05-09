package com.github.im.server

import com.github.im.dto.user.LoginRequest
import com.github.im.server.model.User
import com.github.im.server.repository.UserRepository
import com.github.im.server.service.AuthenticationService
import com.github.im.server.utils.JwtUtil
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.*

class AuthenticationServiceSpec extends Specification {


    @Subject
    AuthenticationService authenticationService
    AuthenticationManager authenticationManager = Mock()
    UserRepository userRepository = Mock()
    JwtUtil jwtUtil = Mock(JwtUtil)

    def setup() {

        authenticationService = new AuthenticationService(authenticationManager, userRepository, jwtUtil)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "testLogin_WithPassword"() {
        given: "A login request with null refresh token"
        def loginRequest = new LoginRequest("testUser", "password", null)
        def authentication = Mock(Authentication)

        def mockUser = Spy(User)
        mockUser.getRefreshToken() >> null
        mockUser.getUserId() >> 1
        mockUser.getUsername() >> "testUser"
        mockUser.getPassword() >> "password"

        authentication.getPrincipal() >> mockUser

        authenticationManager.authenticate(_ as Authentication) >> authentication

        when: "Perform login"
        def result = authenticationService.login(loginRequest)

        then: "Verify the results"
        result.present
        result.get().userId == mockUser.getUserId()
        result.get().token == "mockAccessToken"
        result.get().refreshToken == "mockRefreshToken"

        1 * userRepository.save(_)
        1 * jwtUtil.createToken(mockUser) >> "mockAccessToken"
        // 初次登录需要返回
        1 * jwtUtil.createRefreshToken(mockUser) >> "mockRefreshToken"
    }

    def "testLogin_WithRefreshToken"() {
        given: "A login request with refresh token"
        def loginRequest = new LoginRequest("testUser", "password", "existingRefreshToken")
        def mockUser = Mock(User)
        def authentication = Mock(Authentication)

        authentication.getPrincipal() >> mockUser
        authenticationManager.authenticate(_ as Authentication) >> authentication
        jwtUtil.createToken(mockUser) >> "mockAccessToken"
        mockUser.getRefreshToken() >> "existingRefreshToken"

        when: "Perform login with refresh token"
        def result = authenticationService.login(loginRequest)

        then: "Verify the results"
        result.present
        result.get().token == "mockAccessToken"
        result.get().refreshToken == "existingRefreshToken"

        1 * jwtUtil.createToken(mockUser) >> "mockAccessToken"
        0 * jwtUtil.createRefreshToken(mockUser) >> "existingRefreshToken"
        0 * userRepository.save(_)
    }
}

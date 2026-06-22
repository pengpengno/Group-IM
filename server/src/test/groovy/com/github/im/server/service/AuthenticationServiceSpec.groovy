package com.github.im.server.service

import com.github.im.dto.user.LoginRequest
import com.github.im.server.model.Company
import com.github.im.server.model.User
import com.github.im.server.repository.UserRepository
import com.github.im.server.utils.JwtUtil
import com.github.im.server.utils.UserTokenManager
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.*

import javax.swing.text.html.Option

class AuthenticationServiceSpec extends Specification {


    @Subject
    AuthenticationService authenticationService
    AuthenticationManager authenticationManager = Mock()
    UserRepository userRepository = Mock()
    JwtUtil jwtUtil = Mock(JwtUtil)
    def companyService = Mock(CompanyService)
    def companyUserService = Mock(CompanyUserService)
    def userTokenManager = Mock(UserTokenManager)

    def setup() {

        authenticationService = new AuthenticationService(authenticationManager, userRepository, jwtUtil,userTokenManager,companyService,companyUserService)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "testLogin_WithPassword"() {
        given: "A login request with null refresh token"

        companyService.findById(_ as Long) >> { companyId->
            def company = Spy(Company)
            company.getCompanyId() >> companyId
            company.getActive() >> true
            company.getSchemaName() >> "public"
            return Optional.of(company )
        }
        def loginRequest = new LoginRequest("testUser", "password", null,null)
        def authentication = Mock(Authentication)

        def mockUser = Spy(User)
        mockUser.getRefreshToken() >> null
        mockUser.getUserId() >> 1
        mockUser.getUsername() >> "testUser"
        mockUser.getPassword() >> "password"
        mockUser.getPrimaryCompanyId() >> 1L

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
        1 * userTokenManager.createAccessTokenAndCache(mockUser)  >> "mockAccessToken"
        1 * userTokenManager.createRefreshToken(mockUser)  >> "mockRefreshToken"
    }

    def "testLogin_WithRefreshToken"() {
        given: "A login request with refresh token"
        def loginRequest = new LoginRequest("testUser", "password", "existingRefreshToken","public")
        def mockUser = Mock(User)
        def authentication = Mock(Authentication)

        authentication.getPrincipal() >> mockUser
        authenticationManager.authenticate(_ as Authentication) >> authentication
        jwtUtil.createAccessToken(mockUser) >> "mockAccessToken"
        mockUser.getRefreshToken() >> "existingRefreshToken"

        when: "Perform login with refresh token"
        def result = authenticationService.login(loginRequest)

        then: "Verify the results"
        result.present
        result.get().token == "mockAccessToken"
        result.get().refreshToken == "existingRefreshToken"
        1 * userTokenManager.createAccessTokenAndCache(mockUser)  >> "mockAccessToken"
        0 * userTokenManager.createRefreshToken(mockUser)  >> "mockRefreshToken"

        0 * userRepository.save(_)
    }
}

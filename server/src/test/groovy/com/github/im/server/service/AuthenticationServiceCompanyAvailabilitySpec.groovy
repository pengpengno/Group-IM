package com.github.im.server.service

import com.github.im.server.model.User
import com.github.im.server.repository.UserRepository
import com.github.im.server.utils.JwtUtil
import com.github.im.server.utils.UserTokenManager
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import spock.lang.Specification

class AuthenticationServiceCompanyAvailabilitySpec extends Specification {

    AuthenticationManager authenticationManager = Mock()
    UserRepository userRepository = Mock()
    JwtUtil jwtUtil = Mock()
    UserTokenManager userTokenManager = Mock()
    CompanyService companyService = Mock()
    CompanyUserService companyUserService = Mock()

    AuthenticationService service = new AuthenticationService(
            authenticationManager,
            userRepository,
            jwtUtil,
            userTokenManager,
            companyService,
            companyUserService
    )

    def "admin cannot switch into inactive or unprovisioned company"() {
        given:
        User admin = Mock()
        admin.getUserId() >> 1L
        admin.getUsername() >> "admin"
        companyUserService.isUserInCompany(1L, 42L) >> false
        companyService.findActiveById(42L) >> Optional.empty()

        when:
        service.switchCompany(42L, admin)

        then:
        thrown(BadCredentialsException)
        0 * userTokenManager.createAccessTokenAndCache(_)
    }
}

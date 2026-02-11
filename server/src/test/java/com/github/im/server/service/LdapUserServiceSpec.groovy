package com.github.im.server.service

import com.github.im.server.model.User
import com.github.im.server.repository.UserRepository
import org.springframework.ldap.core.LdapTemplate
import spock.lang.Specification

import javax.naming.Name
import javax.naming.directory.Attributes
import java.time.LocalDateTime

class LdapUserServiceSpec extends Specification {

    LdapTemplate ldapTemplate = Mock()
    UserRepository userRepository = Mock()
    LdapUserService ldapUserService = new LdapUserService(ldapTemplate, userRepository)

    def "test createLdapUser creates user in LDAP and local database"() {
        given: "User details"
        String username = "testuser"
        String password = "password123"
        String email = "testuser@example.com"
        String fullName = "Test User"

        and: "Local user does not exist"
        userRepository.findByUsernameOrEmail(username) >> Optional.empty()

        and: "Saved user"
        User savedUser = User.builder()
                .userId(1L)
                .username(username)
                .email(email)
                .phoneNumber("")
                .passwordHash("")
                .status(true)
                .forcePasswordChange(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        userRepository.save(_) >> savedUser

        when: "creating LDAP user"
        User result = ldapUserService.createLdapUser(username, password, email, fullName)

        then: "user is created in LDAP"
        1 * ldapTemplate.bind(_ as Name, null, _ as Attributes)

        and: "user is created in local database"
        result == savedUser
    }

    def "test createLdapUser uses existing user when already exists in database"() {
        given: "User details"
        String username = "existinguser"
        String password = "password123"
        String email = "existinguser@example.com"
        String fullName = "Existing User"

        and: "User already exists in database"
        User existingUser = User.builder()
                .userId(1L)
                .username(username)
                .email(email)
                .phoneNumber("123456789")
                .passwordHash("somehash")
                .status(true)
                .forcePasswordChange(false)
                .build()
        userRepository.findByUsernameOrEmail(username) >> Optional.of(existingUser)

        when: "creating LDAP user"
        User result = ldapUserService.createLdapUser(username, password, email, fullName)

        then: "user is created in LDAP"
        1 * ldapTemplate.bind(_ as Name, null, _ as Attributes)

        and: "existing user is returned without saving"
        result == existingUser
        0 * userRepository.save(_)
    }

    def "test deleteLdapUser removes user from LDAP"() {
        given: "Username"
        String username = "testuser"

        when: "deleting LDAP user"
        ldapUserService.deleteLdapUser(username)

        then: "user is removed from LDAP"
        1 * ldapTemplate.unbind(_ as Name)
    }
}
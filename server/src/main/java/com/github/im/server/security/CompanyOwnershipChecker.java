package com.github.im.server.security;

import com.github.im.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("companyOwnershipChecker")
public class CompanyOwnershipChecker {

    @Autowired
    private UserRepository userRepository;



}
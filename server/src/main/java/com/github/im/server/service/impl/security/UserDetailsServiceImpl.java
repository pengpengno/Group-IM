package com.github.im.server.service.impl.security;

import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 尝试从本地数据库加载用户
        Optional<User> localUser = userRepository.findByUsernameOrEmail(username);
        if (localUser.isPresent()) {
            return localUser.get();
        }
        
        // 如果在本地数据库中找不到用户，则抛出异常
        throw new UsernameNotFoundException("用户未找到: " + username);
    }
}
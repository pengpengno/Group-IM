package com.github.im.server.service;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * 用户注册逻辑
     */
    public Optional<UserInfo> registerUser(@NotNull RegistrationRequest request) {
        // 验证请求数据
        validateRegistrationRequest(request);

        // 检查用户名或邮箱是否已存在
        userRepository.findByUsernameOrEmail(request.getUsername(), request.getEmail())
                .ifPresent(user -> {
                    throw new IllegalArgumentException("用户已存在！");
                });

        // 创建新用户并返回信息
        User newUser = saveNewUser(request);
        return Optional.of(UserMapper.INSTANCE.userToUserInfo(newUser));
    }



    private User saveNewUser(RegistrationRequest request) {
        // 加密密码并创建用户
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(encryptedPassword)
                .username(request.getUsername())
                .phoneNumber(request.getPhoneNumber())
                .build();

        return userRepository.save(newUser);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户未找到: " + username));

        // 返回 UserDetails 实例
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER") // 假设角色是 "USER"，可以根据需求动态设置
                .build();
    }

    /**
     * 用户登录逻辑，返回用户信息
     */
    public Optional<UserInfo> loginUser(LoginRequest loginRequest) {
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getLoginAccount(),
                loginRequest.getPassword()
        );

        // 验证用户凭证并获取认证结果
        Authentication authResult = authenticationManager.authenticate(authenticationToken);

        // 将认证信息存储到 SecurityContext，以便后续请求中可以访问认证信息
        SecurityContextHolder.getContext().setAuthentication(authResult);

        // 从认证结果中获取用户信息
        User user = (User) authResult.getPrincipal();
        return Optional.of(UserMapper.INSTANCE.userToUserInfo(user));
    }

    /**
     * 直接使用数据库密码验证用户登录
     */
    public Optional<UserInfo> loginUserDirect(LoginRequest loginRequest) {
        return userRepository.findByUsernameOrEmail(loginRequest.getLoginAccount())
                .filter(user -> passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash()))
                .map(UserMapper.INSTANCE::userToUserInfo);
    }


    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 用户密码重置
     */
    public User resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("未找到该用户ID: " + userId));

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }


    /**
     * 注册验证
     * @param request
     */
    private void validateRegistrationRequest(RegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        // 其他的验证逻辑可以根据需求添加
    }
}

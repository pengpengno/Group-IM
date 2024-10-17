package com.github.im.server.service;

import com.github.im.dto.UserInfo;
import com.github.im.dto.UserRegisterRequest;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private  final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;


    public User registerUser(UserRegisterRequest userRegisterRequest) {
        User user = new User();
        user.setUsername(userRegisterRequest.getUsername());
        user.setEmail(userRegisterRequest.getEmail());
        user.setPhoneNumber(userRegisterRequest.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(userRegisterRequest.getPassword())); // 存储加密后的密码
        return userRepository.save(user);
    }


    // 用户登录
    public Optional<UserInfo> loginUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                // 登录成功，返回用户基础信息
                return Optional.of(new UserInfo(user.getUserId(), user.getUsername(), user.getUsername(), user.getAvatarUrl()));
            }
        }
        return Optional.empty(); // 登录失败
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // 其他业务方法（如更新用户信息、查找用户等）


    // 用户密码重置（实现逻辑根据需求可扩展）
    public User resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }
}

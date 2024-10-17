package com.github.im.server.service;

import com.github.im.server.dto.UserInfo;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private  final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;


    public User registerUser(User user) {
        // 注册逻辑，包括加密密码
        user.setPasswordHash(passwordEncoder.encode(user.getPassword()));

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

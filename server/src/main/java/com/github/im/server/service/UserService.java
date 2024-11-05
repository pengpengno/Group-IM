package com.github.im.server.service;

import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.dto.user.UserRegisterRequest;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService  implements UserDetailsService {

    private  final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;


    public Optional<UserInfo> registerUser(RegistrationRequest request) {
        // Validate the request details
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        // Check if username or email already exists
          return userRepository.findByUsernameOrEmail(request.getUsername(), request.getEmail())
                .or(()->
                        // Proceed with registration
                        Optional.of(saveNewUser(request))
                )
                 .map(UserMapper.INSTANCE::userToUserInfo)
         ;
    }

    private  User saveNewUser(RegistrationRequest request) {
        // Encrypt the password and create a new user object
        String encryptedPassword =passwordEncoder.encode(request.getPassword());
        var newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(encryptedPassword)
                .username(request.getUsername())
                .phoneNumber(request.getPhoneNumber())
                .build();
        return userRepository.save(newUser);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 返回 UserDetails 实例
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
//                .roles(user.getRole()) // 假设你的 User 模型有 role 属性
                .build();
    }

    // 用户登录
    public Optional<UserInfo> loginUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                // 登录成功，返回用户基础信息
                return Optional.ofNullable(UserMapper.INSTANCE.userToUserInfo(user));
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

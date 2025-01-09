package com.github.im.server.controller;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import com.github.im.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Valid
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserInfo> registerUser(@RequestBody @Valid RegistrationRequest registrationRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUser(registrationRequest).get());
    }



    @GetMapping("/{username}")
    public Optional<User> getUserByUsername(@PathVariable String username) {
        return userService.findUserByUsername(username);
    }

    // 用户登录
    @PostMapping("/login")
    public ResponseEntity<UserInfo> loginUser(@RequestBody LoginRequest loginRequest) {
        Optional<UserInfo> userInfo = userService.loginUser(loginRequest);
        // 登录失败
        return userInfo.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // 重置用户密码
    @PutMapping("/reset-password/{userId}")
    public User resetPassword(@PathVariable Long userId, @RequestBody String newPassword) {
        return userService.resetPassword(userId, newPassword);
    }


}

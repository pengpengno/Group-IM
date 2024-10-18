package com.github.im.group.gui.api;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.dto.user.UserRegisterRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

/**
 * 用户 端点
 */
@HttpExchange
public interface UserEndpoint {

	// 用户注册
	@PostExchange("/api/users/register")
	Mono<UserRegisterRequest> registerUser(@RequestBody RegistrationRequest registrationRequest);

	// 用户登录
	@PostExchange("/api/users/login")
	Mono<UserInfo> loginUser(@RequestBody LoginRequest loginRequest);

//	// 根据用户名查找用户
//	@GetExchange("/api/users/{username}")
//	Optional<User> getUserByUsername(@PathVariable String username);
//
//	// 重置用户密码
//	@PutExchange("/api/users/reset-password/{userId}")
//	User resetPassword(@PathVariable Long userId, @RequestBody String newPassword);

}
package com.github.im.group.gui.api;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.dto.user.UserRegisterRequest;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 用户 端点
 * <b>
 * <a href="https://www.baeldung.com/spring-6-http-interface"> HttpExchange  REFERENCE DOC</a>
 * </b>
 */
@HttpExchange
public interface UserEndpoint {

	// 用户注册
	@PostExchange("/api/users/register")
	Mono<UserRegisterRequest> registerUser(@RequestBody RegistrationRequest registrationRequest);

	// 用户登录
	@PostExchange("/api/users/login")
	Mono<UserInfo> loginUser(@RequestBody LoginRequest loginRequest);


	@PostExchange("/api/users/query")
	Flux<UserInfo>  findUserByNameOrEmail(@RequestParam String query);

}
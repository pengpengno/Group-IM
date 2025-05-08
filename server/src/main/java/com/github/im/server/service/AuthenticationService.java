package com.github.im.server.service;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.impl.security.RefreshAuthenticationToken;
import com.github.im.server.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService  {

    @Setter
    private AuthenticationManager authenticationManager;


    private final UserRepository userRepository;


    @Autowired
    JwtUtil jwtUtil;


    public Optional<UserInfo> login(LoginRequest loginRequest){

        // 密码登录 和 长期refreshToken 登录
        if (loginRequest.getRefreshToken() == null) {
            return loginUser(loginRequest);
        } else {
            return loginViaRefreshToken(loginRequest.getRefreshToken());
        }

    }

    public Optional<UserInfo> loginUser(LoginRequest loginRequest) {
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getLoginAccount(),
                loginRequest.getPassword()
        );

        Authentication authResult = authenticationManager.authenticate(authenticationToken);

        SecurityContextHolder.getContext()
                .setAuthentication(authResult);

        User user = (User) authResult.getPrincipal();

        var token = jwtUtil.createToken(user);

        var refreshToken = jwtUtil.createRefreshToken(user); // 生成长期 refreshToken

        var userInfo = UserMapper.INSTANCE.userToUserInfo(user);
        userInfo.setToken(token);
        return Optional.of(userInfo);
    }

    public Optional<UserInfo> loginViaRefreshToken(String refreshToken) {
        var authToken = new RefreshAuthenticationToken(refreshToken);
        Authentication authResult = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authResult);

        User user = (User) authResult.getPrincipal();
        String accessToken = jwtUtil.createToken(user);

//        user.setRefreshToken(refreshToken);
//        // 保存 长期 Token
//        userRepository.save(user);
//        String newRefreshToken = jwtUtil.createRefreshToken(user);

        UserInfo userInfo = UserMapper.INSTANCE.userToUserInfo(user);
        userInfo.setToken(accessToken);
        return Optional.of(userInfo);
    }



}

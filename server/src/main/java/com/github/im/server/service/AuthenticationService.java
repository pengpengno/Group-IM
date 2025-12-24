package com.github.im.server.service;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.impl.security.RefreshAuthenticationToken;
import com.github.im.server.utils.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService  {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    private final CompanyService companyService;

    public Optional<UserInfo> login(LoginRequest loginRequest){

        // 密码登录 和 长期refreshToken 登录
        if (loginRequest.getRefreshToken() == null) {
            return loginUser(loginRequest);
        } else {
            return loginViaRefreshToken(loginRequest.getRefreshToken());
        }

    }

    /***
     * 密码登录
     * @param loginRequest
     * @return
     */
    @Transactional
    public Optional<UserInfo> loginUser(LoginRequest loginRequest) {
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getLoginAccount(),
                loginRequest.getPassword()
        );

        Authentication authResult = authenticationManager.authenticate(authenticationToken);

        SecurityContextHolder.getContext()
                .setAuthentication(authResult);

        User user = (User) authResult.getPrincipal();
//        final String companyCode = loginRequest.getCompanyCode();
//        if (companyCode != null && companyCode.equals("public")) {
//            user.setCurrentCompany(companyService.findBySchemaName(companyCode).get());
//        } else {
//            user.setCurrentCompany(companyService.findById(loginRequest.getCompanyId()).get());
//        }
        user.setCurrentCompany(companyService.findById(user.getPrimaryCompanyId()).get());


        val token = jwtUtil.createToken(user);
        val refreshToken = jwtUtil.createRefreshToken(user);
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        var userInfo = UserMapper.INSTANCE.userToUserInfo(user);

        userInfo.setToken(token);
        userInfo.setRefreshToken(refreshToken);

        return Optional.of(userInfo);
    }

    /**
     * 根据长期 Token 登录
     * @param refreshToken 长期有效的刷新令牌
     * @return 用户信息及新的访问令牌
     */
    public Optional<UserInfo> loginViaRefreshToken(String refreshToken) {
        var authToken = new RefreshAuthenticationToken(refreshToken);
        Authentication authResult = authenticationManager.authenticate(authToken);
//        SecurityContextHolder.getContext().setAuthentication(authResult);

        User user = (User) authResult.getPrincipal();
        String accessToken = jwtUtil.createToken(user);

        UserInfo userInfo = UserMapper.INSTANCE.userToUserInfo(user);
        userInfo.setToken(accessToken);
        userInfo.setRefreshToken(user.getRefreshToken());
        return Optional.of(userInfo);
    }
}
package com.github.im.server.service;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.impl.security.RefreshAuthenticationToken;
import com.github.im.server.utils.JwtUtil;
import com.github.im.server.utils.UserTokenManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserTokenManager userTokenManager;
    private final CompanyService companyService;
    private final CompanyUserService companyUserService;

    public Optional<UserInfo> login(LoginRequest loginRequest) {
        if (loginRequest.getRefreshToken() == null) {
            return loginUser(loginRequest);
        }
        return loginViaRefreshToken(loginRequest.getRefreshToken());
    }

    @Transactional
    public Optional<UserInfo> loginUser(LoginRequest loginRequest) {
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getLoginAccount(),
                loginRequest.getPassword()
        );

        Authentication authResult = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authResult);

        User user = (User) authResult.getPrincipal();
        String companyCode = loginRequest.getCompanyCode();
        if ("public".equals(companyCode)) {
            user.setCurrentCompany(companyService.findActiveBySchemaName(companyCode)
                    .orElseThrow(() -> new BadCredentialsException("当前公司" + companyCode + "不存在或不可用！")));
        } else {
            user.setCurrentCompany(companyService.findActiveById(user.getPrimaryCompanyId())
                    .orElseThrow(() -> new BadCredentialsException("当前公司不存在或尚未完成初始化！")));
        }

        val token = userTokenManager.createAccessTokenAndCache(user);
        val refreshToken = userTokenManager.createRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        UserInfo userInfo = UserMapper.INSTANCE.userToUserInfo(user);
        userInfo.setToken(token);
        userInfo.setRefreshToken(refreshToken);
        return Optional.of(userInfo);
    }

    public Optional<UserInfo> loginViaRefreshToken(String refreshToken) {
        RefreshAuthenticationToken authToken = new RefreshAuthenticationToken(refreshToken);
        Authentication authResult = authenticationManager.authenticate(authToken);
        User user = (User) authResult.getPrincipal();

        val accessToken = userTokenManager.createAccessTokenAndCache(user);

        UserInfo userInfo = UserMapper.INSTANCE.userToUserInfo(user);
        userInfo.setToken(accessToken);
        userInfo.setRefreshToken(user.getRefreshToken());
        return Optional.of(userInfo);
    }

    /**
     * Switch current company only when the target company is active. This is part of the
     * provisioning availability boundary: an inactive company cannot be entered even by admin.
     */
    @Transactional
    public Optional<UserInfo> switchCompany(Long companyId, User user) {
        boolean belongsToCompany = companyUserService.isUserInCompany(user.getUserId(), companyId);

        if (!belongsToCompany && !user.getUsername().equals("admin")) {
            throw new BadCredentialsException("您不属于该公司，无法切换！");
        }

        Company company = companyService.findActiveById(companyId)
                .orElseThrow(() -> new BadCredentialsException("目标公司不存在或尚未完成初始化！"));

        user.setCurrentCompany(company);
        String token = userTokenManager.createAccessTokenAndCache(user);

        UserInfo userInfo = UserMapper.INSTANCE.userToUserInfo(user);
        userInfo.setToken(token);
        userInfo.setRefreshToken(user.getRefreshToken());
        return Optional.of(userInfo);
    }
}

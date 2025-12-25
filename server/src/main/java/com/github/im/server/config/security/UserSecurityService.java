package com.github.im.server.config.security;


import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.CompanyService;
import com.github.im.server.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserSecurityService {


    private final UserRepository userRepository;
    private final CompanyService companyService;




    /**
     * 此方法 不验证 JWT 的有效性
     * 只 将 jwt 转化为 user
     * 将 jwt 中 payload 的数据 转化为 user
     * @param jwt
     * @return 处理后的用户给
     */
    public User jwt2User(Jwt jwt) throws BadCredentialsException {
        long userId = Long.parseLong(jwt.getId());
        Optional<User> userOptional = userRepository.findById(userId);

        // 将 JWT 中的 claim 映射为 User 实体
        return userOptional.map(user-> {
            // 从JWT中提取公司ID并设置到用户对象
            Long companyId = jwt.getClaim(JwtUtil.COMPANY_ID_FIELD);
            String schemaCode = jwt.getClaim(JwtUtil.COMPANY_SCHEMA_FIELD);
            if (companyId != null) {
                var companyOpt =  companyService.findById(companyId);
                var company = companyOpt.orElseThrow(()-> new BadCredentialsException("当前公司不存在！"));
                user.setCurrentCompany(company);
                List<Company> companies = user.getCompanies(); // 加载 Company 对象
            }
            return user;
        }).orElseThrow(()-> new BadCredentialsException("Invalid refresh token"));
    }



}

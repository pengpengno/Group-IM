package com.github.im.server.config.security;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.CompanyService;
import com.github.im.server.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public  class JwtToUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository userRepository;
    private final CompanyService companyService;
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 从 JWT 提取角色信息
//        Collection<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);

        long userId = Long.parseLong(jwt.getId());
        Optional<User> userOptional = userRepository.findById(userId);

        // 将 JWT 中的 claim 映射为你的 User 实体
        return userOptional.map(user-> {
            // 从JWT中提取公司ID并设置到用户对象
            Long companyId = jwt.getClaim(JwtUtil.COMPANY_ID_FIELD);
            String schemaCode = jwt.getClaim(JwtUtil.COMPANY_SCHEMA_FIELD);
            if (companyId != null) {
                var companyOpt =  companyService.findById(companyId);
                var company = companyOpt.orElseThrow(()-> new BadCredentialsException("当前公司不存在！"));
                user.setCurrentCompany(company);
                List<Company> companies = user.getCompanies(); // 加载 Company 对象
                SchemaContext.setCurrentTenant(company.getSchemaName());

            }
            return new UsernamePasswordAuthenticationToken(user,user.getPasswordHash(), user.getAuthorities());
        }).orElseThrow(()-> new BadCredentialsException("Invalid refresh token"));
    }

    @SuppressWarnings("unchecked")
    private Collection<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) return List.of();
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}
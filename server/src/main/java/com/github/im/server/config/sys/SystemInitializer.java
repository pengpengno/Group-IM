package com.github.im.server.config.sys;

import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.CompanyService;
import com.github.im.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 系统初始化组件
 * 在应用启动时创建默认的公共公司
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemInitializer implements CommandLineRunner {
    
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserRepository userRepository;
    private final SystemInitializerProperties properties;
    private final StringRedisTemplate redisTemplate;
    
    @Override
    public void run(String... args) throws Exception {
        // 清理缓存以避免序列化问题
        clearCache();
        
        // 异步执行初始化任务
        initializeSystem();
    }
    
    /**
     * 清理Redis缓存
     */
    private void clearCache() {
        try {
            log.info("Clearing Redis cache...");
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            log.info("Redis cache cleared successfully");
        } catch (Exception e) {
            log.warn("Failed to clear Redis cache: {}", e.getMessage());
        }
    }
    
    @Async
    public void initializeSystem() {
        try {
            if (!properties.isEnabled()) {
                log.info("System initialization is disabled, skipping");
                return;
            }
            
            // 检查是否已存在默认的公司
            Optional<Company> companyOpt = companyService.findBySchemaName(properties.getDefaultCompany().getSchemaName());
            Company defaultCompany;
            
            if (companyOpt.isEmpty()) {
                log.info("Creating default company: {}...", properties.getDefaultCompany().getName());
                // 创建默认的公司
                defaultCompany = new Company();
                defaultCompany.setName(properties.getDefaultCompany().getName());
                defaultCompany.setSchemaName(properties.getDefaultCompany().getSchemaName());
                defaultCompany.setActive(properties.getDefaultCompany().isActive());
                
                try {
                    defaultCompany = companyService.save(defaultCompany);
                    log.info("Successfully created default company: {}", properties.getDefaultCompany().getName());
                } catch (Exception e) {
                    log.error("Failed to create default company: {}", properties.getDefaultCompany().getName(), e);
                    return;
                }
            } else {
                defaultCompany = companyOpt.get();
                log.info("Default company: {} already exists, skipping creation", properties.getDefaultCompany().getName());
            }
            
            // 检查并创建管理员用户
            createAdminUser(defaultCompany);
            
        } catch (Exception e) {
            log.error("Unexpected error during system initialization", e);
        }
    }
    
    private void createAdminUser(Company defaultCompany) {
        try {
            boolean adminNotExist = userService.findUserByUsername(properties.getAdminUser().getUsername()).isEmpty();
            if (adminNotExist) {
                User user = new User();
                user.setUsername(properties.getAdminUser().getUsername());
                user.setEmail(properties.getAdminUser().getEmail());
                user.setPhoneNumber(properties.getAdminUser().getPhoneNumber());
                user.setPrimaryCompanyId(defaultCompany.getCompanyId());
                user.setPasswordHash(passwordEncoder.encode(properties.getAdminUser().getPassword()));
                
                User savedUser = userRepository.save(user);
                log.info("Created new user account for admin: {}", savedUser);
            } else {
                log.info("Admin user already exists, skipping creation");
            }
        } catch (Exception e) {
            log.error("Failed to create admin user", e);
        }
    }
}
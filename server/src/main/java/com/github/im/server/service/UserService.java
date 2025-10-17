package com.github.im.server.service;

import com.github.im.common.util.ValidatorUtil;
import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.config.ForcePasswordChangeConfig;
import com.github.im.server.mapstruct.UserMapper;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    private final ForcePasswordChangeConfig forcePasswordChangeConfig;

    /**
     * 用户注册逻辑
     */
    public Optional<UserInfo> registerUser(@NotNull RegistrationRequest request) {
        // 使用 Hibernate 验证
        validateRegistrationRequest(request);

        // 检查用户名或邮箱是否已存在
        userRepository.findByUsernameOrEmail(request.getUsername(), request.getEmail())
                .ifPresent(user -> {
                    throw new IllegalArgumentException("用户已存在！");
                });

        // 创建新用户并返回信息
        User newUser = saveNewUser(request);
        return Optional.of(UserMapper.INSTANCE.userToUserInfo(newUser));
    }

    /**
     * 批量注册用户
     * @param requests 用户注册请求列表
     * @return 已注册用户信息列表
     */
    @Transactional
    public List<UserInfo> batchRegisterUsers(@NotNull List<RegistrationRequest> requests) {
        List<UserInfo> registeredUsers = new ArrayList<>();
        
        // 收集所有请求中的用户名和邮箱，用于批量检查
        List<String> usernames = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        
        for (RegistrationRequest request : requests) {
            usernames.add(request.getUsername());
            emails.add(request.getEmail());
        }
        
        // 检查是否有重复的用户名或邮箱在请求列表中
        if (hasDuplicates(usernames) || hasDuplicates(emails)) {
            throw new IllegalArgumentException("批量注册请求中存在重复的用户名或邮箱！");
        }
        
        // 检查数据库中是否已存在这些用户名或邮箱
        List<User> existingUsers = userRepository.findByUsernameInOrEmailIn(usernames, emails);
        if (!existingUsers.isEmpty()) {
            Map<String, String> existingMap = new HashMap<>();
            for (User user : existingUsers) {
                existingMap.put(user.getUsername(), "用户名已存在");
                existingMap.put(user.getEmail(), "邮箱已存在");
            }
            
            // 构建错误信息
            StringBuilder errorMsg = new StringBuilder("以下用户已存在：");
            for (RegistrationRequest request : requests) {
                if (existingMap.containsKey(request.getUsername())) {
                    errorMsg.append(String.format(" 用户名[%s]已存在；", request.getUsername()));
                }
                if (existingMap.containsKey(request.getEmail())) {
                    errorMsg.append(String.format(" 邮箱[%s]已存在；", request.getEmail()));
                }
            }
            throw new IllegalArgumentException(errorMsg.toString());
        }
        
        // 执行注册
        StringBuilder batchErrors = new StringBuilder();
        for (RegistrationRequest request : requests) {
            try {
                Optional<UserInfo> registeredUser = registerUser(request);
                registeredUser.ifPresent(registeredUsers::add);
            } catch (DataAccessException e) {
                // 处理数据库访问异常，记录错误但继续处理其他用户
                String errorMsg = String.format("注册用户失败: %s, 数据库错误: %s; ", request.getUsername(), e.getMessage());
                batchErrors.append(errorMsg);
                log.warn(errorMsg);
            } catch (Exception e) {
                // 记录单个用户注册失败的日志，但继续处理其他用户
                String errorMsg = String.format("注册用户失败: %s, 错误: %s; ", request.getUsername(), e.getMessage());
                batchErrors.append(errorMsg);
                log.warn(errorMsg);
            }
        }
        
        // 如果有错误，抛出包含所有错误信息的异常
        if (batchErrors.length() > 0) {
            throw new IllegalArgumentException("批量注册完成，但部分用户注册失败: " + batchErrors.toString());
        }
        return registeredUsers;
    }

    /**
     * 检查列表中是否有重复元素
     * @param list 要检查的列表
     * @return 是否有重复元素
     */
    private boolean hasDuplicates(List<String> list) {
        return list.size() != list.stream().distinct().count();
    }

    private User saveNewUser(RegistrationRequest request) {
        // 加密密码并创建用户
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(encryptedPassword)
                .username(request.getUsername())
                .phoneNumber(request.getPhoneNumber())
                .forcePasswordChange(forcePasswordChangeConfig.isForcePasswordChangeEnabled())
                .build();

        return userRepository.save(newUser);
    }



    /**
     * 用户登录逻辑，返回用户信息
     */
    public Optional<UserInfo> loginUser(LoginRequest loginRequest) {

       return  authenticationService.login(loginRequest);

    }

    /**
     * 直接使用数据库密码验证用户登录
     */
    public Optional<UserInfo> loginUserDirect(LoginRequest loginRequest) {
        return userRepository.findByUsernameOrEmail(loginRequest.getLoginAccount())
                .filter(user -> passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash()))
                .map(UserMapper.INSTANCE::userToUserInfo);
    }


    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据用户 queryString  查询其他用户， 不会返回自己
     * @param queryString 用户Username /  email
     * @param authentication  {@code  SecurityContextHolder.getContext().getAuthentication()}
     * @return
     */
    public Page<UserInfo> findUserByQueryStrings(String queryString,Authentication authentication) {
        User user = (User) authentication.getPrincipal();


        return userRepository.findAll((root, query, cb)-> {
            List<Predicate> predicates = new ArrayList<>();
            // 查询 用户名   或者email 相似的

            predicates.add(cb.like(root.get("username"), "%" + queryString + "%"));
            predicates.add(cb.like(root.get("email"), "%" + queryString + "%"));

            // 排除当前用户
            Predicate notCurrentUser = cb.notEqual(root.get("username"), user.getUsername());
            predicates.add(notCurrentUser);
            return cb.or(predicates.toArray(new Predicate[0]))
                    ;
        },Pageable.ofSize(100)).map(UserMapper.INSTANCE::userToUserInfo);

    }

    /***
     *  根据 queryString 查询所有邮箱和用户名符合条件的用户
     * @param queryString
     * @return
     */
    public Page<UserInfo> findUserByQueryStrings(String queryString) {


        return userRepository.findAll((root, query, cb)-> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(root.get("username"), "%" + queryString + "%"));
            predicates.add(cb.like(root.get("email"), "%" + queryString + "%"));
            // 查询 用户名   或者email 相似的
            return cb.or(predicates.toArray(new Predicate[0]))
                    ;
        },Pageable.ofSize(100)).map(UserMapper.INSTANCE::userToUserInfo);
    }




    /**
     * 用户密码重置
     */
    public User resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("未找到该用户ID: " + userId));

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }


    /**
     * 注册验证
     * @param request
     */
    private void validateRegistrationRequest(RegistrationRequest request) {
        List<String> validateErrors = ValidatorUtil.validate(request);
        if (!validateErrors.isEmpty()) {
            throw new IllegalArgumentException("验证失败: " + String.join(", ", validateErrors));
        }
        
        // 验证密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        // 其他的验证逻辑可以根据需求添加
    }


    /**
     * 解析Excel文件并转换为用户注册请求列表
     * @param inputStream Excel文件输入流
     * @return 用户注册请求列表
     * @throws IOException IO异常
     */
    public List<RegistrationRequest> parseExcelFile(InputStream inputStream) throws IOException {
        List<RegistrationRequest> userList = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            // 跳过标题行
            if (row.getRowNum() == 0) continue;

            RegistrationRequest request = new RegistrationRequest();
            
            // 假设Excel列顺序为: username, email, phone
            Cell usernameCell = row.getCell(0);
            Cell emailCell = row.getCell(1);
            Cell phoneCell = row.getCell(2);
            
            if (usernameCell != null) {
                request.setUsername(getCellValueAsString(usernameCell));
            }
            
            if (emailCell != null) {
                request.setEmail(getCellValueAsString(emailCell));
            }
            
            if (phoneCell != null) {
                request.setPhoneNumber(getCellValueAsString(phoneCell));
            }
            
            // 设置默认密码
            request.setPassword(forcePasswordChangeConfig.getDefaultPassword());
            request.setConfirmPassword(forcePasswordChangeConfig.getDefaultPassword());

            userList.add(request);
        }

        workbook.close();
        return userList;
    }

    /**
     * 生成用户导入Excel模板
     * @param outputStream 输出流
     * @throws IOException IO异常
     */
    public void generateUserImportTemplate(OutputStream outputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户导入模板");
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("用户名(username)");
        headerRow.createCell(1).setCellValue("邮箱(email)");
        headerRow.createCell(2).setCellValue("手机号(phoneNumber)");
        
        // 调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        
        // 写入输出流
        workbook.write(outputStream);
        workbook.close();
    }

    /**
     * 获取单元格的字符串值
     * @param cell 单元格
     * @return 字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
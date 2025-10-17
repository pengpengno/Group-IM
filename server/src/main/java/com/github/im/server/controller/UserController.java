package com.github.im.server.controller;

import com.github.im.dto.PageResult;
import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import com.github.im.server.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    /**
     * 批量注册用户
     * @param registrationRequests 用户注册请求列表
     * @return 注册结果列表
     */
    @PostMapping("/batch-register")
    public ResponseEntity<List<UserInfo>> batchRegisterUsers(@RequestBody @Valid List<RegistrationRequest> registrationRequests) {
        List<UserInfo> registeredUsers = userService.batchRegisterUsers(registrationRequests);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUsers);
    }


    @GetMapping("/{username}")
    public Optional<User> getUserByUsername(@PathVariable String username) {
        //TODO 权限
        return userService.findUserByUsername(username);
    }


    /**
     * 根据传入的查询字符串查询用户
     * @param query  查询条件用户名 获取  Email
     * @return 与之对应的信息
     */
    @PostMapping("/query")
    public ResponseEntity<PagedModel<UserInfo>> queryUserByNameOrEmail(@RequestParam String query ,
                                                                       @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(new PagedModel<>(userService.findUserByQueryStrings(query, SecurityContextHolder.getContext().getAuthentication())));
    }


    // 用户登录
    @PostMapping("/login")
    public ResponseEntity<UserInfo> loginUser(@RequestBody @NotNull(message = "request not be null") LoginRequest loginRequest) {
        Optional<UserInfo> userInfo = userService.loginUser(loginRequest);
        // 登录失败
        return userInfo.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }


    // 重置用户密码
    @PutMapping("/reset-password/{userId}")
    public User resetPassword(@PathVariable Long userId, @RequestBody String newPassword) {
        return userService.resetPassword(userId, newPassword);
    }


    /**
     * 从Excel文件导入用户数据
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importUsersFromExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("code", HttpStatus.BAD_REQUEST.value());
            response.put("message", "文件为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<RegistrationRequest> users = userService.parseExcelFile(file.getInputStream());
            userService.batchRegisterUsers(users);
            response.put("code", HttpStatus.OK.value());
            response.put("message", "用户导入成功");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "处理文件失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (IllegalArgumentException e) {
            // 处理业务逻辑异常，如重复数据检查失败
            response.put("code", HttpStatus.BAD_REQUEST.value());
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (DataAccessException e) {
            // 处理数据库访问异常
            response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "数据库操作失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "导入用户时发生未知错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 下载用户导入Excel模板
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @GetMapping("/import-template")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=user_import_template.xlsx");
        userService.generateUserImportTemplate(response.getOutputStream());
    }

}
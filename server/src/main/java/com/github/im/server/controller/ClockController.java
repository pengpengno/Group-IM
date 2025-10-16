package com.github.im.server.controller;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.RegistrationRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import com.github.im.server.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/system")
@Valid
public class ClockController {
    @Autowired
    private UserService userService;

    /**
     * 获取服务器当前时间
     * 客户端进行对时操作
     * @return  系统当前时间戳
     */
    @PostMapping("/clock")
    public ResponseEntity<Long> clock() {

        // 获取 服务器的 时间 ， 进行对时
        return ResponseEntity.ok( System.currentTimeMillis());
    }



}

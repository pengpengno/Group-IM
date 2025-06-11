package com.github.im.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理所有验证异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
            log.warn("Validation failed for field: {} - {}", fieldName, errorMessage);

        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }


    // 处理所有验证异常
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MissingServletRequestParameterException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("message",ex.getMessage());
        log.error(" error processing  ",ex);

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }


    // 处理文件异常
    @ExceptionHandler({IllegalArgumentException.class, FileNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("message",ex.getMessage());
        log.error(" error processing  ",ex);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // 处理文件异常
    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<Map<String, String>> UNAUTHORIZED(BadCredentialsException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("message","用户鉴权验证失败！");
        log.error(" error processing  ",ex);
        return new ResponseEntity<>(errors, HttpStatus.UNAUTHORIZED);
    }



    // 处理所有验证异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleExceptions(Exception ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("message","服务繁忙！");
        log.error(" error processing  ",ex);
        return new ResponseEntity<>(errors, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

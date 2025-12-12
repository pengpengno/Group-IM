package com.github.im.server.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常类
 * 用于处理业务逻辑中的各种异常情况
 */
public class BusinessException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;
    
    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
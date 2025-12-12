package com.github.im.server.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;

/**
 * 统一响应工具类
 * 提供标准的API响应格式和错误处理
 */
public class ResponseUtil {
    
    /**
     * 创建成功的响应
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应格式
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return success("操作成功", data);
    }
    
    /**
     * 创建成功的响应
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应格式
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(Instant.now().toEpochMilli());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建错误响应
     * @param status HTTP状态码
     * @param message 错误消息
     * @return 统一错误响应格式
     */
    public static ResponseEntity<ApiResponse<Object>> error(HttpStatus status, String message) {
        ApiResponse<Object> response = new ApiResponse<>();
        response.setCode(status.value());
        response.setMessage(message);
        response.setTimestamp(Instant.now().toEpochMilli());
        return ResponseEntity.status(status).body(response);
    }
}
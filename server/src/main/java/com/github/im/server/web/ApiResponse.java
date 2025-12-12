package com.github.im.server.web;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.time.Instant;

/**
 * API统一响应封装类
 * @param <T> 响应数据类型
 */
@Data
public class ApiResponse<T> implements Serializable {
    
    /**
     * 响应码
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private long timestamp;
    
    /**
     * 默认构造函数
     */
    public ApiResponse() {
        this.timestamp = Instant.now().toEpochMilli();
    }
    
    /**
     * 构造函数
     * @param code 响应码
     * @param message 响应消息
     */
    public ApiResponse(int code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    /**
     * 构造函数
     * @param code 响应码
     * @param message 响应消息
     * @param data 响应数据
     */
    public ApiResponse(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 创建成功响应
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(HttpStatus.OK.value(), "操作成功");
    }
    
    /**
     * 创建成功响应
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), "操作成功", data);
    }
    
    /**
     * 创建成功响应
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, data);
    }
    
    /**
     * 创建错误响应
     * @param status HTTP状态码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(status.value(), message);
    }
    
    /**
     * 创建错误响应
     * @param code 响应码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message);
    }
}
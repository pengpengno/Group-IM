package com.github.im.server.config;

import com.github.im.server.exception.BusinessException;
import com.github.im.server.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.FileNotFoundException;
import java.net.URI;
import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), ex.getStatus(), request);
    }

    // 处理非法参数异常
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return handleExceptionInternal(ex, "参数错误: " + ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    // 处理数据库访问异常
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccessException(DataAccessException ex, WebRequest request) {
        return handleExceptionInternal(ex, "数据库访问异常", new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    // 处理文件相关异常
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Object> handleFileNotFoundException(FileNotFoundException ex, WebRequest request) {
        return handleExceptionInternal(ex, "文件未找到: " + ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    // 处理认证异常
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex, WebRequest request) {
        return handleExceptionInternal(ex, "认证失败", new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    // 处理参数验证异常
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        });
        
        return handleExceptionInternal(ex, "参数验证失败: " + errors.toString(), headers, status, request);
    }
    
    // 处理通用异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex, WebRequest request) {
        return handleExceptionInternal(ex, "服务器内部错误: " + ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // 如果body已经是ProblemDetail，则直接使用
        if (body instanceof ProblemDetail) {
            return ResponseEntity.status(statusCode).headers(headers).body(body);
        }

        // 创建ProblemDetail
        ProblemDetail problemDetail = ProblemDetail.forStatus(statusCode);
        problemDetail.setTitle(getTitleForStatus(statusCode));
        problemDetail.setDetail(body != null ? body.toString() : "服务器内部错误");
        // 添加instance属性
        String requestDescription = request.getDescription(false);

        // instance：当前请求 URI（非常重要）
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest httpRequest = servletWebRequest.getRequest();
            String uri = httpRequest.getRequestURI();
            problemDetail.setInstance(URI.create(uri));
        }

        // 添加时间戳和错误代码
        problemDetail.setProperty("timestamp", Instant.now().toEpochMilli());
        
        if (ex instanceof BusinessException) {
            problemDetail.setProperty("errorCode", ((BusinessException) ex).getErrorCode());
        }

        // 根据客户端期望的内容类型决定返回格式
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json") && !acceptHeader.contains("application/problem+json")) {
            // 客户端期望JSON格式，返回ApiResponse
            ApiResponse<Object> apiResponse = new ApiResponse<>(
                statusCode.value(),
                body != null ? body.toString() : "服务器内部错误"
            );
            apiResponse.setTimestamp(Instant.now().toEpochMilli());
            return ResponseEntity.status(statusCode).headers(headers).body(apiResponse);
        } else {
            // 默认返回ProblemDetail
            return ResponseEntity.status(statusCode).headers(headers).body(problemDetail);
        }
    }

    /**
     * 根据HTTP状态码获取标题
     * @param status HTTP状态码
     * @return 标题
     */
    private String getTitleForStatus(HttpStatusCode status) {
        if (status.equals(HttpStatus.BAD_REQUEST)) {
            return "Bad Request";
        } else if (status.equals(HttpStatus.UNAUTHORIZED)) {
            return "Unauthorized";
        } else if (status.equals(HttpStatus.NOT_FOUND)) {
            return "Not Found";
        } else if (status.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
            return "Internal Server Error";
        } else {
            return "Error";
        }
    }
}
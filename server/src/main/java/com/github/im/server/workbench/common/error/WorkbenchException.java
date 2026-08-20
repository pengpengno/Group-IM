package com.github.im.server.workbench.common.error;

import com.github.im.server.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class WorkbenchException extends BusinessException {

    public WorkbenchException(HttpStatus status, WorkbenchErrorCode errorCode, String message) {
        super(status, errorCode.code(), message);
    }

    public static WorkbenchException unauthorized(String message) {
        return new WorkbenchException(HttpStatus.UNAUTHORIZED, WorkbenchErrorCode.AUTHENTICATION_REQUIRED, message);
    }

    public static WorkbenchException forbidden(WorkbenchErrorCode errorCode, String message) {
        return new WorkbenchException(HttpStatus.FORBIDDEN, errorCode, message);
    }

    public static WorkbenchException badRequest(WorkbenchErrorCode errorCode, String message) {
        return new WorkbenchException(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public static WorkbenchException conflict(WorkbenchErrorCode errorCode, String message) {
        return new WorkbenchException(HttpStatus.CONFLICT, errorCode, message);
    }

    public static WorkbenchException notFound(WorkbenchErrorCode errorCode, String message) {
        return new WorkbenchException(HttpStatus.NOT_FOUND, errorCode, message);
    }
}

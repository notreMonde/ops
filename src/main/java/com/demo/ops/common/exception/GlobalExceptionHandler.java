package com.demo.ops.common.exception;

import com.demo.ops.common.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 使用 @RestControllerAdvice 统一拦截各类异常，并返回标准化的 ApiResponse 响应体。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 处理资源未找到异常，返回 404 */
    @ExceptionHandler(DemoNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(DemoNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    /** 处理请求参数校验失败异常，返回 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "请求参数校验失败"));
    }

    /** 处理其他未捕获的通用异常，返回 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage()));
    }
}

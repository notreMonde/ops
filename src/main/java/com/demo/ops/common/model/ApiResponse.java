package com.demo.ops.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 统一 API 响应封装类。
 * 所有 REST 接口均使用此类包裹返回数据，提供统一的 code、message、timestamp 和 data 结构。
 *
 * @param <T> 响应数据类型
 */
public class ApiResponse<T> {

    /** 状态码，200 表示成功 */
    private final int code;
    /** 响应消息 */
    private final String message;
    /** 响应时间戳，格式为 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;
    /** 响应数据 */
    private final T data;

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    /** 创建成功响应 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /** 创建错误响应 */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public T getData() {
        return data;
    }
}

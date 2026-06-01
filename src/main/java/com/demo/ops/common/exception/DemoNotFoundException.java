package com.demo.ops.common.exception;

/**
 * 自定义未找到异常。
 * 当请求的资源或数据不存在时抛出，由全局异常处理器捕获后返回 404 响应。
 */
public class DemoNotFoundException extends RuntimeException {

    public DemoNotFoundException(String message) {
        super(message);
    }
}

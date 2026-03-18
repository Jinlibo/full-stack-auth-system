package com.product.app.common;

import lombok.Getter;

/**
 * 业务异常类，用于在业务逻辑层抛出带有自定义状态码的运行时异常。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    /**
     * 构造带有错误码和错误信息的业务异常。
     *
     * @param code    业务错误码
     * @param message 错误描述信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造默认错误码（500）的业务异常。
     *
     * @param message 错误描述信息
     */
    public BusinessException(String message) {
        this(500, message);
    }
}

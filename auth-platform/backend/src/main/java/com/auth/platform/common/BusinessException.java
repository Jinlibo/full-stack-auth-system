package com.auth.platform.common;

import lombok.Getter;

/**
 * 业务异常类
 *
 * <p>用于表示系统中可预期的业务逻辑错误，例如：用户名已存在、记录不存在、权限不足等。
 * 继承自 {@link RuntimeException}，为非受检异常，无需在方法签名中声明。
 *
 * <p>与普通系统异常的区别：
 * <ul>
 *   <li>业务异常是开发者主动抛出的，属于正常业务流程的一部分</li>
 *   <li>由 {@link GlobalExceptionHandler} 捕获后以 warn 级别记录，不打印堆栈</li>
 *   <li>携带自定义状态码 code，用于前端区分不同类型的错误</li>
 * </ul>
 *
 * @author auth-platform
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务状态码，与 HTTP 状态码语义保持一致（如 400、401、403、404、500）
     */
    private final int code;

    /**
     * 构造带自定义状态码和错误消息的业务异常
     *
     * @param code    业务状态码（如 400、404 等）
     * @param message 错误描述信息（建议使用中文，直接展示给用户）
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造默认状态码（500）的业务异常
     *
     * <p>适用于未分类的业务错误场景，状态码默认为 500。
     *
     * @param message 错误描述信息
     */
    public BusinessException(String message) {
        this(500, message);
    }
}

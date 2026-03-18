package com.product.app.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/**
 * 全局异常处理器，统一拦截并处理业务异常、权限异常、参数校验异常及未知系统异常。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，返回对应的业务错误码和错误信息。
     *
     * @param e 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBiz(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理访问拒绝异常，返回 403 无权限响应。
     *
     * @param e 访问拒绝异常
     * @return 统一响应结果
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleAccess(AccessDeniedException e) {
        return R.fail(403, "无权限");
    }

    /**
     * 处理请求参数校验失败异常，汇总所有字段的校验错误信息后返回。
     *
     * @param e 方法参数校验异常
     * @return 统一响应结果，包含校验错误详情
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        return R.fail(400, msg);
    }

    /**
     * 处理所有未被捕获的系统异常，记录错误日志并返回 500 响应。
     *
     * @param e 未知异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleEx(Exception e) {
        log.error("系统异常: ", e);
        return R.fail("系统内部错误");
    }
}

package com.product.app.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应结果封装类，包含响应码、提示信息和业务数据。
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
public class R<T> {

    /** 响应状态码 */
    private int code;

    /** 响应提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /**
     * 构建带数据的成功响应（状态码 200）。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应结果
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    /**
     * 构建无数据的成功响应（状态码 200）。
     *
     * @param <T> 数据类型
     * @return 成功响应结果
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * 构建带自定义错误码的失败响应。
     *
     * @param code    错误状态码
     * @param message 错误提示信息
     * @param <T>     数据类型
     * @return 失败响应结果
     */
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /**
     * 构建默认错误码（500）的失败响应。
     *
     * @param message 错误提示信息
     * @param <T>     数据类型
     * @return 失败响应结果
     */
    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }
}

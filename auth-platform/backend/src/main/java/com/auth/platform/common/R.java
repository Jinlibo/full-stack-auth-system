package com.auth.platform.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应结构体（Response Wrapper）
 *
 * <p>所有 Controller 方法的返回值都应包装在此对象中，前端通过 code 字段判断请求是否成功。
 *
 * <p>JSON 示例：
 * <pre>
 * // 成功（有数据）
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... }
 * }
 *
 * // 成功（无数据）
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": null
 * }
 *
 * // 业务失败
 * {
 *   "code": 400,
 *   "message": "用户名已存在",
 *   "data": null
 * }
 *
 * // 未认证
 * {
 *   "code": 401,
 *   "message": "Token 已过期",
 *   "data": null
 * }
 * </pre>
 *
 * <p>状态码约定（与 HTTP 状态码含义保持一致）：
 * <ul>
 *   <li>200：操作成功</li>
 *   <li>400：客户端参数错误（如参数校验失败、业务规则冲突）</li>
 *   <li>401：未认证（Token 不存在、已过期、已被注销）</li>
 *   <li>403：无权限（Token 有效但权限不足）</li>
 *   <li>404：资源不存在</li>
 *   <li>500：服务器内部错误</li>
 * </ul>
 *
 * <p>泛型参数 T：业务数据的实际类型，如 UserInfo、IPage&lt;SysRole&gt; 等。
 * 无返回数据时使用 R&lt;Void&gt;，data 字段值为 null。
 *
 * @param <T> 响应数据的类型
 * @author auth-platform
 */
@Data              // Lombok：自动生成 getter、setter、toString、equals、hashCode
@NoArgsConstructor // Lombok：生成无参构造函数（Jackson 反序列化时需要）
public class R<T> {

    /**
     * 业务状态码
     * 非 HTTP 状态码，但语义保持一致（见类注释中的状态码约定）
     */
    private int code;

    /**
     * 响应消息
     * 成功时固定为 "success"；失败时为具体的错误描述（中文，直接展示给用户）
     */
    private String message;

    /**
     * 响应业务数据
     * 成功时为实际数据对象；操作无返回值时为 null（R&lt;Void&gt;）
     */
    private T data;

    /**
     * 构建成功响应（有数据）
     *
     * <p>使用方式：
     * <pre>return R.ok(userInfo);</pre>
     *
     * @param <T>  数据类型
     * @param data 要返回的业务数据对象
     * @return code=200、message="success"、data=data 的响应对象
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    /**
     * 构建成功响应（无数据）
     *
     * <p>适用于增、删、改等无需返回数据的操作。
     * <pre>return R.ok();</pre>
     *
     * @param <T> 类型参数（实际为 Void）
     * @return code=200、message="success"、data=null 的响应对象
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * 构建失败响应（自定义状态码）
     *
     * <p>使用方式：
     * <pre>return R.fail(400, "用户名已存在");</pre>
     *
     * @param <T>     类型参数
     * @param code    业务状态码（参见类注释中的状态码约定）
     * @param message 错误描述（建议使用中文，直接展示给前端用户）
     * @return 失败响应对象（data=null）
     */
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /**
     * 构建失败响应（默认状态码 500）
     *
     * <p>适用于未预期的服务器内部错误。
     * <pre>return R.fail("系统内部错误");</pre>
     *
     * @param <T>     类型参数
     * @param message 错误描述
     * @return code=500 的失败响应对象
     */
    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }
}

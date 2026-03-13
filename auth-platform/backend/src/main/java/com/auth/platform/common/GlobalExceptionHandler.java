package com.auth.platform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * <p>通过 {@link RestControllerAdvice} 注解，拦截所有 Controller 层抛出的异常，
 * 统一封装为 {@link R} 响应格式返回给前端，避免在每个 Controller 方法中重复编写 try-catch。
 *
 * <p>异常处理优先级（Spring 按照方法的异常类型与实际异常的继承关系就近匹配）：
 * <ol>
 *   <li>{@link BusinessException}：业务逻辑异常（最高优先级，最具体）</li>
 *   <li>{@link BadCredentialsException}：Spring Security 认证失败（用户名/密码错误）</li>
 *   <li>{@link AccessDeniedException}：Spring Security 权限不足</li>
 *   <li>{@link MethodArgumentNotValidException}：{@code @RequestBody} 参数校验失败</li>
 *   <li>{@link BindException}：{@code @ModelAttribute} 或 GET 参数绑定校验失败</li>
 *   <li>{@link Exception}：兜底处理，捕获所有未预期异常（最低优先级）</li>
 * </ol>
 *
 * <p>日志策略：
 * <ul>
 *   <li>业务异常：warn 级别（预期内的用户操作错误，不需要堆栈跟踪）</li>
 *   <li>系统异常：error 级别，输出完整堆栈（需要开发者排查）</li>
 * </ul>
 *
 * @author auth-platform
 */
@Slf4j            // Lombok：注入 log 对象（SLF4J Logger）
@RestControllerAdvice // @ControllerAdvice + @ResponseBody：对所有 @Controller 生效，返回 JSON
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（{@link BusinessException}）
     *
     * <p>业务异常是开发者主动抛出的可预期异常（如"用户名已存在"、"记录不存在"等），
     * 这类异常是正常业务流程的一部分，不需要打印完整堆栈，使用 warn 级别日志即可。
     *
     * <p>HTTP 状态码：使用 BusinessException 中携带的 code 字段（200/400/401/403/404 等）。
     * 注意：此方法未使用 {@code @ResponseStatus}，HTTP 状态码仍为 200，
     * 错误信息通过响应体的 code 字段传递（前端响应拦截器通过 res.code !== 200 判断）。
     *
     * @param e 业务异常（包含自定义状态码和错误消息）
     * @return 封装了错误信息的统一响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Spring Security 认证失败异常（{@link BadCredentialsException}）
     *
     * <p>当用户提交的密码与数据库中存储的 BCrypt 哈希不匹配时，
     * {@code DaoAuthenticationProvider} 抛出此异常。
     *
     * <p>安全最佳实践：响应消息不区分"用户名不存在"和"密码错误"，
     * 统一返回"用户名或密码错误"，防止用户名枚举攻击。
     *
     * <p>{@code @ResponseStatus(UNAUTHORIZED)}：同时设置 HTTP 状态码为 401，
     * 让前端可以通过 HTTP 状态码而非响应体判断认证失败。
     *
     * @param e Spring Security 凭证错误异常
     * @return code=401 的失败响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<?> handleBadCredentials(BadCredentialsException e) {
        // 不记录详细日志，避免安全审计日志中出现大量无效的枚举攻击记录
        return R.fail(401, "用户名或密码错误");
    }

    /**
     * 处理 Spring Security 权限不足异常（{@link AccessDeniedException}）
     *
     * <p>当通过了认证（有效 Token）但没有访问特定资源所需权限时，
     * Spring Security 的 {@code FilterSecurityInterceptor} 抛出此异常。
     * 常见触发场景：Controller 方法上的 {@code @PreAuthorize("hasAuthority('xxx')")} 校验失败。
     *
     * @param e Spring Security 权限拒绝异常
     * @return code=403 的失败响应（HTTP 状态码也为 403）
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleAccessDenied(AccessDeniedException e) {
        return R.fail(403, "权限不足，无法访问该资源");
    }

    /**
     * 处理 {@code @RequestBody} 参数校验失败异常（{@link MethodArgumentNotValidException}）
     *
     * <p>当 Controller 方法参数使用 {@code @Valid} + {@code @RequestBody} 时，
     * Jakarta Validation 对 DTO 中的 {@code @NotBlank}、{@code @Email}、{@code @Size} 等注解
     * 进行校验失败后，Spring 会抛出此异常。
     *
     * <p>错误信息处理：将所有字段的校验错误合并为一条字符串（"字段名: 错误原因; 字段名2: 原因"），
     * 方便前端在开发模式下快速定位问题。
     *
     * @param e 参数校验失败异常，包含所有字段的 FieldError 列表
     * @return code=400 的失败响应，message 为所有校验错误的拼接
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidation(MethodArgumentNotValidException e) {
        // 将所有字段校验错误信息拼接，格式："username: 不能为空; email: 格式不正确"
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("请求参数校验失败");
        return R.fail(400, msg);
    }

    /**
     * 处理 GET 请求参数绑定校验失败异常（{@link BindException}）
     *
     * <p>当 Controller 方法参数使用 {@code @Valid} + 对象类型（非 {@code @RequestBody}，
     * 如 GET 请求的查询参数）校验失败时，Spring 抛出 {@link BindException}。
     * 例如：{@code public R<IPage<SysRole>> pageRoles(@Valid PageQuery query)}
     * 且 PageQuery 中有 {@code @Min(1) int pageNum}，传入 pageNum=0 时触发。
     *
     * @param e 绑定异常（含字段错误列表）
     * @return code=400 的失败响应
     */
    @ExceptionHandler(BindException.class)
    public R<?> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("请求参数绑定失败");
        return R.fail(400, msg);
    }

    /**
     * 兜底异常处理器（处理所有未被上方方法捕获的异常）
     *
     * <p>这是最低优先级的处理器，捕获系统中未预期的异常（如数据库连接失败、NPE 等）。
     * 此类异常属于系统 Bug，需要记录完整的错误堆栈供开发者排查。
     *
     * <p>安全原则：不向前端暴露详细的堆栈信息（可能包含系统内部结构等敏感信息），
     * 仅返回通用的"系统内部错误"提示，详细信息只写入服务器日志。
     *
     * @param e 任意未捕获异常
     * @return code=500 的失败响应（HTTP 状态码也为 500）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleException(Exception e) {
        // error 级别：记录完整堆栈，方便开发者通过日志排查问题
        log.error("系统内部异常: ", e);
        // 对外隐藏具体错误原因，只给用户一个通用提示
        return R.fail(500, "系统内部错误，请联系管理员");
    }
}

package com.auth.platform.security;

import com.auth.platform.entity.SysUser;
import com.auth.platform.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security UserDetailsService 实现类
 *
 * <p>{@link UserDetailsService} 是 Spring Security 核心接口，
 * 用于根据用户名加载用户认证信息（密码、权限列表等）。
 *
 * <p>本类被以下两处调用：
 * <ol>
 *   <li><b>登录认证</b>：{@code AuthenticationManager.authenticate()} 内部会调用此方法
 *       根据用户名加载用户，然后与提交的密码进行 BCrypt 比对</li>
 *   <li><b>JWT 请求认证</b>：{@link JwtAuthenticationFilter} 在每次 API 请求时
 *       调用此方法根据 Token 中的 username 重新加载 UserDetails，
 *       以获取最新的权限列表（角色/权限可能在运行时变更）</li>
 * </ol>
 *
 * <p>返回的 {@link LoginUser} 对象实现了 {@link UserDetails} 接口，
 * Spring Security 会从中获取：
 * <ul>
 *   <li>{@code getPassword()}：BCrypt 加密后的密码（用于登录时对比）</li>
 *   <li>{@code getAuthorities()}：权限集合（由角色标识列表构建 GrantedAuthority）</li>
 *   <li>{@code isEnabled()}、{@code isAccountNonLocked()} 等：账号状态检查</li>
 * </ul>
 *
 * @author auth-platform
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * 用户 Mapper，用于查询用户信息及其关联的角色和权限
     * 包含自定义 SQL 方法：selectRoleKeysByUserId / selectPermissionKeysByUserId
     */
    private final SysUserMapper userMapper;

    /**
     * 根据用户名加载用户详细信息（Spring Security 核心回调）
     *
     * <p>查询流程：
     * <ol>
     *   <li>按用户名精确查询 sys_user 表</li>
     *   <li>若用户不存在，抛出 {@link UsernameNotFoundException}
     *       （Spring Security 会将其转换为认证失败响应）</li>
     *   <li>若账号被禁用（status != 1），同样抛出异常阻止登录</li>
     *   <li>查询用户关联的角色标识列表（通过 sys_user_role + sys_role 联表）</li>
     *   <li>查询用户关联的权限标识列表（通过角色进一步关联 sys_permission）</li>
     *   <li>封装为 {@link LoginUser} 对象返回</li>
     * </ol>
     *
     * <p>性能注意：此方法在每次 JWT 认证时都会被调用（3 次数据库查询），
     * 高并发场景下建议引入本地缓存（如 Caffeine）缓存 UserDetails。
     *
     * @param username 用户名（来自登录表单或 JWT Token 的 username claim）
     * @return 包含用户信息和权限的 {@link UserDetails}（实际为 {@link LoginUser}）
     * @throws UsernameNotFoundException 用户不存在或账号被禁用时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 第一步：根据用户名精确查询用户（LambdaQueryWrapper 使用方法引用，避免字段名硬编码）
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));

        // 第二步：验证用户是否存在
        if (user == null) {
            // 注意：安全最佳实践中，"用户不存在"和"密码错误"的提示语应保持一致
            // 防止攻击者通过不同的错误信息枚举系统中的有效用户名
            // 但本项目为管理后台，此处给出明确提示方便调试
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 第三步：验证账号状态
        if (user.getStatus() != 1) {
            // 账号被管理员禁用时，阻止登录
            // Spring Security 会将此异常包装为认证失败（由 GlobalExceptionHandler 统一处理）
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 第四步：查询该用户拥有的角色标识列表
        // SQL：SELECT r.role_key FROM sys_role r
        //      INNER JOIN sys_user_role ur ON r.id = ur.role_id
        //      WHERE ur.user_id = #{userId} AND r.status = 1
        List<String> roles = userMapper.selectRoleKeysByUserId(user.getId());

        // 第五步：查询该用户（通过其角色）拥有的权限标识列表
        // SQL：SELECT DISTINCT p.permission_key FROM sys_permission p
        //      INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        //      INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
        //      WHERE ur.user_id = #{userId}
        List<String> permissions = userMapper.selectPermissionKeysByUserId(user.getId());

        // 第六步：封装为 LoginUser（自定义的 UserDetails 实现），包含完整的认证信息
        // LoginUser 内部会将 roles 和 permissions 合并为 GrantedAuthority 集合
        return new LoginUser(user, roles, permissions);
    }
}

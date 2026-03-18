package com.auth.platform.security;

import com.auth.platform.entity.SysUser;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security 登录用户信息封装类
 * <p>实现 {@link UserDetails} 接口，是 Spring Security 认证体系中的核心用户对象。
 * 由 {@link UserDetailsServiceImpl#loadUserByUsername} 构建并返回，
 * 包含用户实体数据、角色列表和权限列表，用于登录验证和接口权限控制。</p>
 */
@Data
public class LoginUser implements UserDetails {

    /** 系统用户实体，包含用户名、密码、账号状态等基本信息 */
    private SysUser user;
    /** 用户拥有的角色标识列表，如 ["SUPER_ADMIN", "USER"] */
    private List<String> roles;
    /** 用户拥有的权限标识列表，如 ["system:user:add", "product:list"] */
    private List<String> permissions;

    /**
     * 构造登录用户对象
     *
     * @param user        系统用户实体
     * @param roles       用户角色标识列表
     * @param permissions 用户权限标识列表
     */
    public LoginUser(SysUser user, List<String> roles, List<String> permissions) {
        this.user = user;
        this.roles = roles;
        this.permissions = permissions;
    }

    /**
     * 获取用户的权限集合（Spring Security 核心回调）
     * <p>将角色列表和权限列表合并为 {@link GrantedAuthority} 集合：
     * <ul>
     *   <li>角色以 "ROLE_" 前缀添加，如 "ROLE_SUPER_ADMIN"（Spring Security 约定）</li>
     *   <li>权限标识直接添加，如 "system:user:add"</li>
     * </ul></p>
     *
     * @return 包含角色和权限的授权集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 角色添加 "ROLE_" 前缀，符合 Spring Security 的 hasRole() 约定
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        // 权限标识直接添加，对应 @PreAuthorize("hasAuthority('xxx')") 注解使用
        permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
        return authorities;
    }

    /**
     * 获取用户密码（BCrypt 加密后的密文）
     *
     * @return BCrypt 加密后的密码哈希
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * 获取用户名（登录名）
     *
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * 判断账号是否未过期（本系统不启用账号过期机制，始终返回 true）
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 判断账号是否未被锁定（基于账号状态字段判断）
     *
     * @return 账号状态为 1（正常）时返回 true，否则返回 false
     */
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == 1;
    }

    /**
     * 判断凭证（密码）是否未过期（本系统不启用密码过期机制，始终返回 true）
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 判断账号是否启用（基于账号状态字段判断）
     *
     * @return 账号状态为 1（正常）时返回 true，否则返回 false
     */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}

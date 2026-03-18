package com.product.app.security;

import com.product.app.entity.AppUser;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security 登录用户信息封装类，实现 UserDetails 接口，
 * 持有用户实体、角色列表和权限列表，并将其转换为 Spring Security 的 GrantedAuthority。
 */
@Data
public class LoginUser implements UserDetails {

    /** 本地用户实体 */
    private AppUser user;

    /** 用户拥有的角色标识列表 */
    private List<String> roles;

    /** 用户拥有的权限标识列表 */
    private List<String> permissions;

    /**
     * 构造登录用户对象。
     *
     * @param user        本地用户实体
     * @param roles       角色标识列表
     * @param permissions 权限标识列表
     */
    public LoginUser(AppUser user, List<String> roles, List<String> permissions) {
        this.user = user;
        this.roles = roles;
        this.permissions = permissions;
    }

    /**
     * 将角色和权限列表转换为 Spring Security 的授权信息集合。
     * 角色以 ROLE_ 前缀包装，权限直接使用标识键。
     *
     * @return 授权信息集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> auth = new ArrayList<>();
        roles.forEach(r -> auth.add(new SimpleGrantedAuthority("ROLE_" + r)));
        permissions.forEach(p -> auth.add(new SimpleGrantedAuthority(p)));
        return auth;
    }

    /**
     * 返回用户的加密密码。
     *
     * @return BCrypt 加密后的密码
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * 返回用户名。
     *
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * 账号是否未过期，始终返回 true。
     *
     * @return true
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账号是否未锁定，当用户状态为 1（正常）时返回 true。
     *
     * @return 用户状态是否正常
     */
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == 1;
    }

    /**
     * 凭证是否未过期，始终返回 true。
     *
     * @return true
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账号是否可用，当用户状态为 1（正常）时返回 true。
     *
     * @return 用户状态是否正常
     */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}

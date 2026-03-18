package com.product.app.security;

import com.product.app.entity.AppUser;
import com.product.app.mapper.AppUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService 实现类，根据用户名从数据库加载用户信息，
 * 并组装角色和权限列表，返回 LoginUser 供认证框架使用。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserMapper userMapper;

    /**
     * 根据用户名加载用户认证信息，包含用户实体、角色列表和权限列表。
     *
     * @param username 用户名
     * @return 包含完整认证信息的 LoginUser 对象
     * @throws UsernameNotFoundException 用户不存在或账号已禁用时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, username));
        if (user == null) throw new UsernameNotFoundException("用户不存在: " + username);
        if (user.getStatus() != 1) throw new UsernameNotFoundException("用户已禁用");
        var roles = userMapper.selectRoleKeysByUserId(user.getId());
        var perms = userMapper.selectPermissionKeysByUserId(user.getId());
        return new LoginUser(user, roles, perms);
    }
}

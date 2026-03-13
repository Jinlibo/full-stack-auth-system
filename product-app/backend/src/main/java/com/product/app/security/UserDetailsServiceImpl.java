package com.product.app.security;

import com.product.app.entity.AppUser;
import com.product.app.mapper.AppUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final AppUserMapper userMapper;

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

package com.auth.platform.service.impl;

import com.auth.platform.common.BusinessException;
import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.*;
import com.auth.platform.entity.SysUser;
import com.auth.platform.entity.SysUserRole;
import com.auth.platform.mapper.SysUserMapper;
import com.auth.platform.mapper.SysUserRoleMapper;
import com.auth.platform.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统用户管理服务实现（SysUserService Implementation）
 *
 * <p>继承自 {@link ServiceImpl}，自动获得 MyBatis-Plus 提供的基础 CRUD 方法
 * （getById、save、updateById、removeById 等）。
 * 在基础方法之上，补充了业务层所需的：
 * <ul>
 *   <li>分页查询（含关键字搜索和角色/权限关联查询）</li>
 *   <li>用户创建（含密码加密和角色分配）</li>
 *   <li>用户更新（含角色全量替换）</li>
 *   <li>密码修改（需要验证旧密码）</li>
 *   <li>用户删除（保护超级管理员不被删除）</li>
 * </ul>
 *
 * @author auth-platform
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 用户 Mapper：提供用户的基础 CRUD 和自定义联表查询
     * （包含 selectRoleKeysByUserId 和 selectPermissionKeysByUserId 自定义方法）
     */
    private final SysUserMapper userMapper;

    /**
     * 用户角色关联 Mapper：管理 sys_user_role 中间表
     * （deleteByUserId 是自定义方法，用于全量替换用户角色）
     */
    private final SysUserRoleMapper userRoleMapper;

    /**
     * BCrypt 密码加密器：用于加密新密码和验证旧密码
     * Spring Security 自动配置（在 SecurityConfig 中声明为 Bean）
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表（含关键字搜索）
     *
     * <p>查询逻辑：
     * <ul>
     *   <li>若 keyword 不为空，在 username、nickname、email 三个字段中模糊搜索</li>
     *   <li>按 created_at 倒序排列（最新注册的用户显示在最前面）</li>
     *   <li>对每个用户额外查询其角色和权限列表（N+1 问题，可优化为 JOIN 查询）</li>
     * </ul>
     *
     * <p>性能注意：当用户量较大时，每条记录都会额外发起 2 次查询（角色、权限），
     * 建议后续优化为 IN 查询批量获取。
     *
     * @param query 分页查询参数（pageNum、pageSize、keyword）
     * @return 分页结果，records 为 {@link UserInfo} 列表，含角色和权限信息
     */
    @Override
    public IPage<UserInfo> pageUsers(PageQuery query) {
        // 构造分页对象（当前页，每页条数）
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());

        // 构造查询条件（LambdaQueryWrapper 使用方法引用，避免字段名字符串硬编码）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            // 关键字搜索：在用户名、昵称、邮箱三个字段中进行 LIKE 模糊匹配（OR 关系）
            wrapper.like(SysUser::getUsername, query.getKeyword())
                    .or().like(SysUser::getNickname, query.getKeyword())
                    .or().like(SysUser::getEmail, query.getKeyword());
        }
        // 按注册时间倒序，最新注册的用户排在前面
        wrapper.orderByDesc(SysUser::getCreatedAt);

        // 执行分页查询（MyBatis-Plus 自动注入分页拦截器，生成 LIMIT 语句）
        IPage<SysUser> userPage = userMapper.selectPage(page, wrapper);

        // 将 SysUser 实体列表转换为 UserInfo DTO 列表（同时附加角色和权限信息）
        // IPage.convert() 保留分页元信息（total、current、size），只转换 records 列表
        return userPage.convert(user -> {
            UserInfo info = new UserInfo();
            info.setId(user.getId());
            info.setUsername(user.getUsername());
            info.setEmail(user.getEmail());
            info.setPhone(user.getPhone());
            info.setNickname(user.getNickname());
            info.setAvatar(user.getAvatar());
            info.setStatus(user.getStatus());
            // 额外查询该用户的角色列表（selectRoleKeysByUserId 是 XML Mapper 中的自定义 SQL）
            info.setRoles(userMapper.selectRoleKeysByUserId(user.getId()));
            // 额外查询该用户的权限列表
            info.setPermissions(userMapper.selectPermissionKeysByUserId(user.getId()));
            return info;
        });
    }

    /**
     * 查询单个用户信息（含角色和权限）
     *
     * @param userId 用户 ID
     * @return 用户信息 DTO（含角色和权限列表）
     * @throws BusinessException 用户不存在时抛出 404 异常
     */
    @Override
    public UserInfo getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setPhone(user.getPhone());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setStatus(user.getStatus());
        info.setRoles(userMapper.selectRoleKeysByUserId(userId));
        info.setPermissions(userMapper.selectPermissionKeysByUserId(userId));
        return info;
    }

    /**
     * 创建新用户（含密码加密和角色分配）
     *
     * <p>事务说明：{@code @Transactional} 确保"插入用户 + 插入角色关联"两步操作的原子性。
     * 若角色分配失败，用户创建也会回滚，避免出现"有用户无角色"的数据不一致状态。
     *
     * @param request 用户创建请求（username、password、nickname、email、phone、roleIds）
     * @throws BusinessException 用户名已存在时抛出 400 异常
     */
    @Override
    @Transactional
    public void createUser(UserCreateRequest request) {
        // 检查用户名唯一性（数据库有唯一索引，此处提前校验以给出友好错误提示）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) throw new BusinessException(400, "用户名已存在");

        // 创建用户实体
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        // 密码加密：BCrypt 会自动生成随机 salt 并嵌入哈希结果中
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        // 昵称不填时默认使用用户名
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        // 新用户默认状态为正常
        user.setStatus(1);
        userMapper.insert(user);
        // 执行 insert 后，user.getId() 会被 MyBatis-Plus 自动填充为数据库生成的主键

        // 批量分配角色（若 roleIds 为空或 null，则跳过，用户将无任何角色）
        if (!CollectionUtils.isEmpty(request.getRoleIds())) {
            request.getRoleIds().forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            });
        }
    }

    /**
     * 更新用户信息
     *
     * <p>更新策略：
     * <ul>
     *   <li>基础字段（email、phone、nickname、avatar、status）：非 null 字段才更新（selective update）</li>
     *   <li>角色列表（roleIds）：非 null 时执行全量替换（先删除所有旧关联，再插入新关联）</li>
     *   <li>username 和 password 不允许通过此接口修改（密码修改使用专用接口）</li>
     * </ul>
     *
     * <p>{@code @Transactional} 确保"更新用户 + 更新角色关联"的原子性。
     *
     * @param userId  要更新的用户 ID
     * @param request 更新请求（仅包含需要修改的字段，null 字段不覆盖原值）
     * @throws BusinessException 用户不存在时抛出 404 异常
     */
    @Override
    @Transactional
    public void updateUser(Long userId, UserUpdateRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        // 仅更新请求中非 null 的字段（partial update 模式）
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        userMapper.updateById(user);

        // 角色全量替换：若请求中包含 roleIds（即使是空数组），则重置用户角色
        if (request.getRoleIds() != null) {
            // 第一步：删除该用户的所有旧角色关联（自定义 Mapper 方法）
            userRoleMapper.deleteByUserId(userId);
            // 第二步：批量插入新的角色关联
            request.getRoleIds().forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            });
        }
    }

    /**
     * 删除用户
     *
     * <p>安全保护：禁止删除 id=1 的超级管理员，防止系统无人管理。
     *
     * <p>注意：当前实现仅删除 sys_user 记录，
     * sys_user_role 表的关联记录依赖数据库外键约束的级联删除
     * 或需要手动清理（取决于建表时的 ON DELETE 配置）。
     *
     * @param userId 要删除的用户 ID
     * @throws BusinessException 尝试删除超级管理员（id=1）时抛出
     */
    @Override
    public void deleteUser(Long userId) {
        // 硬编码保护超级管理员（id=1），防止所有管理员账号被删除导致系统无法管理
        if (userId == 1L) throw new BusinessException("不能删除超级管理员账号");
        userMapper.deleteById(userId);
    }

    /**
     * 修改用户密码（需要验证旧密码）
     *
     * <p>安全性设计：
     * <ul>
     *   <li>必须提供正确的旧密码才能修改，防止 Token 泄露后的密码劫持</li>
     *   <li>使用 BCrypt 的 matches() 方法对比（而非加密后比对字符串），
     *       因为 BCrypt 每次生成的哈希值不同（含随机 salt）</li>
     *   <li>新密码加密后覆盖存储，BCrypt 自动生成新的 salt</li>
     * </ul>
     *
     * @param userId  要修改密码的用户 ID（从当前登录用户 Token 中获取）
     * @param request 包含 oldPassword（旧密码）和 newPassword（新密码）
     * @throws BusinessException 用户不存在或旧密码不正确时抛出
     */
    @Override
    public void changePassword(Long userId, PasswordChangeRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        // 验证旧密码：BCrypt.matches(明文密码, 数据库中的哈希)
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码不正确");
        }

        // 加密新密码并更新
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }
}

package com.auth.platform.service.impl;

import com.auth.platform.common.BusinessException;
import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.RoleRequest;
import com.auth.platform.entity.SysRole;
import com.auth.platform.entity.SysRolePermission;
import com.auth.platform.mapper.SysRoleMapper;
import com.auth.platform.mapper.SysRolePermissionMapper;
import com.auth.platform.service.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统角色管理服务实现（SysRoleService Implementation）
 *
 * <p>继承自 {@link ServiceImpl}，自动获得 MyBatis-Plus 提供的基础 CRUD 方法。
 * 在基础方法之上，扩展了以下功能：
 * <ul>
 *   <li>分页查询（含关键字搜索）</li>
 *   <li>角色创建（含权限节点关联）</li>
 *   <li>角色更新（含权限关联全量替换）</li>
 *   <li>角色删除（保护超级管理员角色）</li>
 *   <li>查询角色已关联的权限节点 ID 列表</li>
 * </ul>
 *
 * @author auth-platform
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    /** 角色 Mapper，提供对 sys_role 表的基础 CRUD 操作 */
    private final SysRoleMapper roleMapper;
    /** 角色-权限关联 Mapper，管理 sys_role_permission 中间表 */
    private final SysRolePermissionMapper rolePermissionMapper;

    /**
     * 分页查询角色列表（含关键字搜索）
     *
     * <p>支持按角色名称（roleName）和角色标识（roleKey）模糊搜索（OR 关系），
     * 结果按排序序号（sortOrder）升序排列。</p>
     *
     * @param query 分页查询参数（pageNum、pageSize、keyword）
     * @return 分页角色列表
     */
    @Override
    public IPage<SysRole> pageRoles(PageQuery query) {
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            // 在角色名称和角色标识两个字段中进行模糊搜索（OR 关系）
            wrapper.like(SysRole::getRoleName, query.getKeyword())
                    .or().like(SysRole::getRoleKey, query.getKeyword());
        }
        // 按排序序号升序，数值越小越靠前
        wrapper.orderByAsc(SysRole::getSortOrder);
        return roleMapper.selectPage(page, wrapper);
    }

    /**
     * 查询所有启用状态的角色列表（不分页）
     * <p>仅返回 status=1（启用）的角色，按排序序号升序排列，用于下拉框等全量展示场景。</p>
     *
     * @return 所有启用角色的列表
     */
    @Override
    public List<SysRole> listAllRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSortOrder));
    }

    /**
     * 创建角色（同时关联权限节点）
     *
     * <p>创建流程：
     * <ol>
     *   <li>校验 roleKey 唯一性，重复则抛出 400 异常</li>
     *   <li>插入角色记录，默认状态为启用（status=1）</li>
     *   <li>若请求中携带 permissionIds，则批量插入角色-权限关联记录</li>
     * </ol></p>
     *
     * @param request 角色创建请求（roleName、roleKey、sortOrder、remark、permissionIds）
     */
    @Override
    @Transactional
    public void createRole(RoleRequest request) {
        // 检查角色标识唯一性（roleKey 用于 Spring Security 权限控制，不能重复）
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, request.getRoleKey()));
        if (count > 0) throw new BusinessException(400, "角色标识已存在");

        // 创建角色实体并填充字段
        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        // 排序序号未指定时默认为 0
        role.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        role.setRemark(request.getRemark());
        // 新创建的角色默认为启用状态
        role.setStatus(1);
        roleMapper.insert(role);

        // 若指定了权限节点 ID 列表，则批量插入角色-权限关联记录
        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            request.getPermissionIds().forEach(permId -> {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            });
        }
    }

    /**
     * 更新角色信息（同时更新权限关联）
     *
     * <p>更新流程：
     * <ol>
     *   <li>验证角色是否存在，不存在则抛出 404 异常</li>
     *   <li>更新角色基本信息</li>
     *   <li>若请求中包含 permissionIds（即使为空数组），则全量替换权限关联（先删后插）</li>
     * </ol></p>
     *
     * @param roleId  要更新的角色 ID
     * @param request 更新请求
     */
    @Override
    @Transactional
    public void updateRole(Long roleId, RoleRequest request) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException(404, "角色不存在");

        // 更新角色基本信息
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        if (request.getSortOrder() != null) role.setSortOrder(request.getSortOrder());
        role.setRemark(request.getRemark());
        roleMapper.updateById(role);

        // 若请求中携带 permissionIds，则执行权限关联的全量替换
        if (request.getPermissionIds() != null) {
            // 先删除该角色的所有旧权限关联
            rolePermissionMapper.deleteByRoleId(roleId);
            // 再批量插入新的权限关联
            request.getPermissionIds().forEach(permId -> {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            });
        }
    }

    /**
     * 删除角色（同时清除权限关联）
     * <p>安全保护：禁止删除 id=1 的超级管理员角色，防止系统失去最高权限管理入口。</p>
     *
     * @param roleId 要删除的角色 ID
     */
    @Override
    public void deleteRole(Long roleId) {
        // 硬编码保护超级管理员角色（id=1）
        if (roleId == 1L) throw new BusinessException("不能删除超级管理员角色");
        roleMapper.deleteById(roleId);
        // 同步清除该角色的所有权限关联记录
        rolePermissionMapper.deleteByRoleId(roleId);
    }

    /**
     * 获取指定角色已关联的权限节点 ID 列表
     * <p>用于角色编辑页面回显已勾选的权限节点（权限树的选中状态）。</p>
     *
     * @param roleId 角色 ID
     * @return 该角色关联的权限节点 ID 列表
     */
    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).toList();
    }
}

package com.auth.platform.controller;

import com.auth.platform.common.PageQuery;
import com.auth.platform.common.R;
import com.auth.platform.dto.RoleRequest;
import com.auth.platform.entity.SysRole;
import com.auth.platform.service.SysRoleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器（SysRole Controller）
 *
 * <p>提供角色（Role）的 CRUD REST 接口，以及角色权限关联的查询接口。
 * 基础路径：/api/roles
 *
 * <p>接口权限控制（Method Security）：
 * <ul>
 *   <li>查询接口：需要 {@code system:role:query} 权限</li>
 *   <li>新增接口：需要 {@code system:role:add} 权限</li>
 *   <li>编辑接口：需要 {@code system:role:edit} 权限</li>
 *   <li>删除接口：需要 {@code system:role:delete} 权限</li>
 *   <li>获取所有角色（用于下拉）：不限权限，已登录即可访问</li>
 *   <li>获取角色权限列表：不限权限，用于角色编辑对话框中加载已有权限</li>
 * </ul>
 *
 * <p>{@code @PreAuthorize} 注解由 Spring Security 的方法安全拦截器处理，
 * 需要在 SecurityConfig 上添加 {@code @EnableMethodSecurity} 注解才能生效。
 *
 * @author auth-platform
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class SysRoleController {

    /**
     * 角色管理服务，包含角色 CRUD 和权限分配的业务逻辑
     */
    private final SysRoleService roleService;

    /**
     * 分页查询角色列表
     *
     * <p>GET /api/roles?pageNum=1&pageSize=10
     * 需要当前用户拥有 system:role:query 权限（通过 @PreAuthorize 校验）
     *
     * @param query 分页参数（pageNum、pageSize，通过 URL 查询字符串传入）
     * @return 分页角色数据（records 包含角色基本信息，total 为总数）
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:query')")
    public R<IPage<SysRole>> pageRoles(PageQuery query) {
        return R.ok(roleService.pageRoles(query));
    }

    /**
     * 获取所有角色（不分页，用于下拉选择框）
     *
     * <p>GET /api/roles/all
     * 无需特定权限（已登录即可访问），供用户管理页面的"角色"多选框使用
     *
     * @return 完整的角色列表（通常数量不多，全量返回不会有性能问题）
     */
    @GetMapping("/all")
    public R<List<SysRole>> listAllRoles() {
        return R.ok(roleService.listAllRoles());
    }

    /**
     * 获取指定角色已绑定的权限 ID 列表
     *
     * <p>GET /api/roles/{id}/permissions
     * 无需特定权限，供角色编辑对话框使用：打开编辑框时预先勾选已分配的权限。
     *
     * <p>返回值为权限 ID（Long 类型）的列表，前端 el-tree 通过 default-checked-keys
     * 属性将这些 ID 对应的权限节点预先勾选。
     *
     * @param id 角色 ID（路径变量）
     * @return 该角色拥有的权限 ID 列表（如 [1L, 2L, 5L, 8L]）
     */
    @GetMapping("/{id}/permissions")
    public R<List<Long>> getRolePermissions(@PathVariable Long id) {
        return R.ok(roleService.getRolePermissionIds(id));
    }

    /**
     * 创建新角色
     *
     * <p>POST /api/roles
     * 请求体：{
     * roleName:      角色名称
     * roleKey:       角色标识（全局唯一，如 "ADMIN"）
     * remark:        备注
     * permissionIds: 要授予该角色的权限 ID 列表
     * }
     *
     * @param request 角色创建请求，@Valid 触发 Jakarta Validation 校验
     * @return 无 data 的成功响应
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public R<Void> createRole(@Valid @RequestBody RoleRequest request) {
        roleService.createRole(request);
        return R.ok();
    }

    /**
     * 更新角色信息及权限分配
     *
     * <p>PUT /api/roles/{id}
     * 请求体与 createRole 相同。
     * 后端会先删除该角色的所有旧权限关联，再插入新的权限关联（全量替换策略）。
     *
     * <p>注意：roleKey（角色标识）不允许修改，即使请求中携带了 roleKey，
     * 后端也应忽略此字段的更新（防止权限注解引用失效）。
     *
     * @param id      路径变量，要更新的角色 ID
     * @param request 角色更新请求
     * @return 无 data 的成功响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        roleService.updateRole(id, request);
        return R.ok();
    }

    /**
     * 删除角色
     *
     * <p>DELETE /api/roles/{id}
     * 删除角色同时会删除该角色的所有权限关联记录（sys_role_permission 表）。
     *
     * <p>注意：删除角色不会立即影响持有该角色的用户的 JWT Token（Token 内的角色信息是静态的），
     * 需等待 Token 过期或用户重新登录后才会生效。
     *
     * @param id 要删除的角色 ID（路径变量）
     * @return 无 data 的成功响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public R<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }
}

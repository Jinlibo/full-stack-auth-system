package com.auth.platform.controller;

import com.auth.platform.common.R;
import com.auth.platform.dto.PermissionRequest;
import com.auth.platform.service.SysPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器（SysPermission Controller）
 *
 * <p>提供系统权限节点的 CRUD REST 接口。
 * 基础路径：/api/permissions
 *
 * <p>权限节点类型说明：
 * <ul>
 *   <li>type=1（菜单）：对应前端路由和侧边栏导航项</li>
 *   <li>type=2（按钮）：对应页面内的操作按钮</li>
 *   <li>type=3（API）：对应后端接口访问权限，与 @PreAuthorize 注解中的标识对应</li>
 * </ul>
 *
 * <p>接口权限控制：
 * <ul>
 *   <li>getPermissionTree：无需特定权限（已登录即可），角色分配也需要此接口</li>
 *   <li>createPermission / updatePermission / deletePermission：需要 system:permission 权限</li>
 * </ul>
 *
 * @author auth-platform
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    /**
     * 权限管理服务，包含权限树构建和 CRUD 业务逻辑
     */
    private final SysPermissionService permissionService;

    /**
     * 获取完整权限树
     *
     * <p>GET /api/permissions/tree
     * 返回递归嵌套的树形结构，已按 sortOrder 字段升序排列。
     *
     * <p>无需特定权限（已登录即可访问），因为：
     * <ol>
     *   <li>权限管理页面需要此接口展示所有权限节点</li>
     *   <li>角色编辑对话框中的权限选择树也需要此接口</li>
     * </ol>
     *
     * <p>返回值为 {@code List<Map<String, Object>>} 而非实体类，
     * 是因为 SysPermission 实体不含 children 字段，
     * 使用 Map 可以灵活地在运行时动态添加 children 属性。
     *
     * @return 树形权限列表（顶级节点的 parentId 为 0，有子节点才包含 children 字段）
     */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> getPermissionTree() {
        return R.ok(permissionService.getPermissionTree());
    }

    /**
     * 创建权限节点
     *
     * <p>POST /api/permissions
     * 请求体：{
     * permissionName:  权限名称（展示用，如"用户列表"）
     * permissionKey:   权限标识（全局唯一，如"system:user:query"）
     * parentId:        父权限 ID（0=顶级权限）
     * type:            类型（1=菜单，2=按钮，3=API）
     * path:            路由或接口路径（可选）
     * icon:            图标名称（可选）
     * sortOrder:       排序号（同级中排序）
     * }
     *
     * <p>后端校验：permissionKey 在 sys_permission 表中必须唯一。
     *
     * @param request 权限创建请求，@Valid 触发参数校验
     * @return 无 data 的成功响应
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:permission')")
    public R<Void> createPermission(@Valid @RequestBody PermissionRequest request) {
        permissionService.createPermission(request);
        return R.ok();
    }

    /**
     * 更新权限节点信息
     *
     * <p>PUT /api/permissions/{id}
     * 请求体同 createPermission，允许修改所有字段（包括 permissionKey）。
     *
     * <p>注意：修改 permissionKey 会影响所有在 @PreAuthorize 中引用了该标识的接口，
     * 以及角色的权限关联记录，操作前需确认影响范围。
     *
     * @param id      要更新的权限节点 ID（路径变量）
     * @param request 权限更新请求
     * @return 无 data 的成功响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission')")
    public R<Void> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        permissionService.updatePermission(id, request);
        return R.ok();
    }

    /**
     * 删除权限节点
     *
     * <p>DELETE /api/permissions/{id}
     * 后端会检查该节点是否存在子权限，有子权限时拒绝删除并返回错误。
     * 必须先删除所有子权限节点，才能删除父节点（保证树结构完整性）。
     *
     * @param id 要删除的权限节点 ID（路径变量）
     * @return 无 data 的成功响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission')")
    public R<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return R.ok();
    }
}

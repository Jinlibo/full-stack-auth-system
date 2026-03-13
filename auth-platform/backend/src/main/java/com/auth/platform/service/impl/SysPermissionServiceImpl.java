package com.auth.platform.service.impl;

import com.auth.platform.common.BusinessException;
import com.auth.platform.dto.PermissionRequest;
import com.auth.platform.entity.SysPermission;
import com.auth.platform.mapper.SysPermissionMapper;
import com.auth.platform.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限管理服务实现（SysPermissionService Implementation）
 *
 * <p>继承自 {@link ServiceImpl}，获得 MyBatis-Plus 的基础 CRUD 方法。
 * 主要扩展了权限树的构建逻辑和权限节点的 CRUD 操作。
 *
 * <p>权限数据模型（平铺 → 树形）：
 * <pre>
 * 数据库中权限以平铺方式存储（每行一个节点，parentId 指向父节点）：
 *   id=1, name="系统管理",  parentId=0  (顶级菜单)
 *   id=2, name="用户管理",  parentId=1  (子菜单)
 *   id=3, name="新增用户",  parentId=2  (按钮)
 *   id=4, name="删除用户",  parentId=2  (按钮)
 *
 * 构建后的树形结构：
 *   系统管理 (id=1)
 *     └─ 用户管理 (id=2)
 *           ├─ 新增用户 (id=3)
 *           └─ 删除用户 (id=4)
 * </pre>
 *
 * @author auth-platform
 */
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    /**
     * 权限 Mapper，提供对 sys_permission 表的 CRUD 操作
     */
    private final SysPermissionMapper permissionMapper;

    /**
     * 获取完整权限树
     *
     * <p>实现思路：
     * <ol>
     *   <li>一次性查询所有权限记录（按 sortOrder 排序）</li>
     *   <li>在内存中递归构建树形结构（避免多次数据库查询）</li>
     *   <li>返回 Map 列表而非实体列表（因为实体类不含 children 字段）</li>
     * </ol>
     *
     * <p>时间复杂度：O(n²)（外层遍历 × 每层 filter）。对于权限节点数量（通常 &lt;200 个），
     * 此复杂度完全可接受，无需优化为 O(n) 的 HashMap 方式。
     *
     * @return 递归嵌套的树形权限列表（顶级节点 parentId=0）
     */
    @Override
    public List<Map<String, Object>> getPermissionTree() {
        // 一次性加载所有权限，按 sortOrder 升序排列（保证树的展示顺序）
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSortOrder));
        // 从 parentId=0 的顶级节点开始，递归构建树
        return buildTree(all, 0L);
    }

    /**
     * 递归构建权限树
     *
     * <p>算法：
     * <ol>
     *   <li>从 all 列表中过滤出所有 parentId 等于当前 parentId 的节点（当前层级的节点）</li>
     *   <li>对每个节点，构建 Map 表示（包含节点的所有属性）</li>
     *   <li>递归调用自身，以当前节点的 id 作为新的 parentId，构建子树</li>
     *   <li>若子树非空，将其作为 "children" 加入当前节点的 Map（叶子节点无 children 字段）</li>
     * </ol>
     *
     * <p>使用 {@link LinkedHashMap} 而非 {@link HashMap}，保证字段的插入顺序在 JSON 序列化时保持一致，
     * 提升接口响应的可读性（id 在前，children 在最后）。
     *
     * @param all      全量权限列表（平铺结构）
     * @param parentId 当前要构建的层级的父节点 ID（顶层调用时为 0）
     * @return 当前层级的节点 Map 列表（已嵌套子节点）
     */
    private List<Map<String, Object>> buildTree(List<SysPermission> all, Long parentId) {
        return all.stream()
                // 过滤出属于当前层级的节点（parentId 匹配）
                // 使用 Objects.equals 而非 == 比较 Long 对象，避免自动拆箱的 NullPointerException
                .filter(p -> Objects.equals(p.getParentId(), parentId))
                .map(p -> {
                    // 使用 LinkedHashMap 保证字段在 JSON 序列化时的顺序（id → name → ... → children）
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", p.getId());
                    node.put("permissionName", p.getPermissionName());
                    node.put("permissionKey", p.getPermissionKey());
                    node.put("parentId", p.getParentId());
                    node.put("type", p.getType());        // 1=菜单，2=按钮，3=API
                    node.put("path", p.getPath());        // 路由路径或接口 URL
                    node.put("icon", p.getIcon());        // 图标名称
                    node.put("sortOrder", p.getSortOrder());   // 排序号
                    node.put("status", p.getStatus());      // 状态（1=正常）

                    // 递归构建子节点列表（以当前节点 id 为 parentId）
                    List<Map<String, Object>> children = buildTree(all, p.getId());

                    // 仅当子节点不为空时才添加 children 字段（叶子节点不含此字段）
                    // 这样前端 el-tree 和 el-table 可以正确判断是否为叶子节点
                    if (!children.isEmpty()) {
                        node.put("children", children);
                    }
                    return node;
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建权限节点
     *
     * <p>校验逻辑：
     * <ul>
     *   <li>permissionKey 全局唯一（用于 @PreAuthorize 注解引用，重复会导致权限混乱）</li>
     * </ul>
     *
     * <p>新权限节点默认状态为正常（status=1）。
     *
     * @param request 权限创建请求
     * @throws BusinessException permissionKey 已存在时抛出 400 异常
     */
    @Override
    public void createPermission(PermissionRequest request) {
        // 检查权限标识唯一性
        Long count = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getPermissionKey, request.getPermissionKey()));
        if (count > 0) throw new BusinessException(400, "权限标识 [" + request.getPermissionKey() + "] 已存在");

        // 创建权限实体并填充字段
        SysPermission perm = new SysPermission();
        perm.setPermissionName(request.getPermissionName());
        perm.setPermissionKey(request.getPermissionKey());
        perm.setParentId(request.getParentId());   // 0 表示顶级权限
        perm.setType(request.getType());           // 1=菜单，2=按钮，3=API
        perm.setPath(request.getPath());           // 路由或接口路径
        perm.setIcon(request.getIcon());           // 菜单图标
        perm.setSortOrder(request.getSortOrder()); // 同级排序
        perm.setStatus(1);                         // 默认启用
        permissionMapper.insert(perm);
    }

    /**
     * 更新权限节点信息
     *
     * <p>所有字段均可修改（包括 permissionKey，但需谨慎操作）。
     * 若修改了 permissionKey，需要同步更新所有在代码中硬编码引用该标识的 @PreAuthorize 注解。
     *
     * @param id      要更新的权限节点 ID
     * @param request 更新内容
     * @throws BusinessException 权限节点不存在时抛出 404 异常
     */
    @Override
    public void updatePermission(Long id, PermissionRequest request) {
        SysPermission perm = permissionMapper.selectById(id);
        if (perm == null) throw new BusinessException(404, "权限节点不存在");

        // 更新所有可修改字段（全量更新，所有字段均覆盖）
        perm.setPermissionName(request.getPermissionName());
        perm.setPermissionKey(request.getPermissionKey());
        perm.setParentId(request.getParentId());
        perm.setType(request.getType());
        perm.setPath(request.getPath());
        perm.setIcon(request.getIcon());
        perm.setSortOrder(request.getSortOrder());
        permissionMapper.updateById(perm);
    }

    /**
     * 删除权限节点
     *
     * <p>保护机制：存在子权限时拒绝删除，防止产生孤儿数据（子节点的 parentId 指向不存在的父节点）。
     * 用户必须先删除所有子权限，才能删除父权限节点。
     *
     * <p>注意：删除权限节点不会自动清理 sys_role_permission 表中的关联记录，
     * 若需要级联清理，可在此处添加相应的 delete 操作（或依赖数据库外键级联）。
     *
     * @param id 要删除的权限节点 ID
     * @throws BusinessException 存在子权限节点时拒绝删除
     */
    @Override
    public void deletePermission(Long id) {
        // 检查是否存在以此节点为父节点的子权限
        Long childCount = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("该权限节点下存在 " + childCount + " 个子权限，请先删除子权限");
        }
        permissionMapper.deleteById(id);
    }
}

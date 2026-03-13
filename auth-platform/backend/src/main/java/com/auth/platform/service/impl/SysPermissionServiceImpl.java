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

@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    private final SysPermissionMapper permissionMapper;

    @Override
    public List<Map<String, Object>> getPermissionTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder));
        return buildTree(all, 0L);
    }

    private List<Map<String, Object>> buildTree(List<SysPermission> all, Long parentId) {
        return all.stream()
                .filter(p -> Objects.equals(p.getParentId(), parentId))
                .map(p -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", p.getId());
                    node.put("permissionName", p.getPermissionName());
                    node.put("permissionKey", p.getPermissionKey());
                    node.put("parentId", p.getParentId());
                    node.put("type", p.getType());
                    node.put("path", p.getPath());
                    node.put("icon", p.getIcon());
                    node.put("sortOrder", p.getSortOrder());
                    node.put("status", p.getStatus());
                    List<Map<String, Object>> children = buildTree(all, p.getId());
                    if (!children.isEmpty()) node.put("children", children);
                    return node;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void createPermission(PermissionRequest request) {
        Long count = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getPermissionKey, request.getPermissionKey()));
        if (count > 0) throw new BusinessException(400, "权限标识已存在");

        SysPermission perm = new SysPermission();
        perm.setPermissionName(request.getPermissionName());
        perm.setPermissionKey(request.getPermissionKey());
        perm.setParentId(request.getParentId());
        perm.setType(request.getType());
        perm.setPath(request.getPath());
        perm.setIcon(request.getIcon());
        perm.setSortOrder(request.getSortOrder());
        perm.setStatus(1);
        permissionMapper.insert(perm);
    }

    @Override
    public void updatePermission(Long id, PermissionRequest request) {
        SysPermission perm = permissionMapper.selectById(id);
        if (perm == null) throw new BusinessException(404, "权限不存在");

        perm.setPermissionName(request.getPermissionName());
        perm.setPermissionKey(request.getPermissionKey());
        perm.setParentId(request.getParentId());
        perm.setType(request.getType());
        perm.setPath(request.getPath());
        perm.setIcon(request.getIcon());
        perm.setSortOrder(request.getSortOrder());
        permissionMapper.updateById(perm);
    }

    @Override
    public void deletePermission(Long id) {
        Long childCount = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getParentId, id));
        if (childCount > 0) throw new BusinessException("存在子权限,不能删除");
        permissionMapper.deleteById(id);
    }
}

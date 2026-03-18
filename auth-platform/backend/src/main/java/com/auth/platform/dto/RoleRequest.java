package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色创建/更新请求数据传输对象
 * <p>封装创建或修改系统角色时前端提交的数据，支持同时为角色关联权限节点列表。</p>
 */
@Data
public class RoleRequest {
    /** 角色显示名称，不能为空，如"超级管理员"、"普通用户" */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;
    /** 角色唯一标识符，不能为空，如"SUPER_ADMIN"、"USER"，用于权限控制中的角色匹配 */
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;
    /** 排序序号，数值越小越靠前，可为空（默认使用 0） */
    private Integer sortOrder;
    /** 角色备注说明，可为空 */
    private String remark;
    /** 要关联的权限节点 ID 列表，更新时将全量替换原有权限关联 */
    private List<Long> permissionIds;
}

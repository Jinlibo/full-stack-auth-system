package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 权限节点创建/更新请求数据传输对象
 * <p>封装新增或修改权限节点时前端提交的数据，支持菜单、按钮、API 三种权限类型。</p>
 */
@Data
public class PermissionRequest {
    /** 权限名称（显示名称），不能为空，如"用户管理"、"新增用户" */
    @NotBlank(message = "权限名称不能为空")
    private String permissionName;
    /** 权限标识（唯一标识符），不能为空，如"system:user:add"，用于 @PreAuthorize 注解引用 */
    @NotBlank(message = "权限标识不能为空")
    private String permissionKey;
    /** 父节点 ID，默认为 0 表示顶级权限节点 */
    private Long parentId = 0L;
    /** 权限类型：1=菜单，2=按钮，3=API 接口，默认为 1 */
    private Integer type = 1;
    /** 路由路径或接口 URL，菜单类型时为前端路由地址 */
    private String path;
    /** 菜单图标名称，仅菜单类型权限使用 */
    private String icon;
    /** 同级节点排序序号，数值越小越靠前，默认为 0 */
    private Integer sortOrder = 0;
}

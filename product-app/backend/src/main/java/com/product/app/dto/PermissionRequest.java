package com.product.app.dto;

import lombok.Data;

@Data
public class PermissionRequest {
    private String permissionName;
    private String permissionKey;
    private Long parentId;
    private Integer type; // 1=菜单, 2=按钮, 3=API
    private String path;
    private Integer sortOrder;
    private Integer status;
}

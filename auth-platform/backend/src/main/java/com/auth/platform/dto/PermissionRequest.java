package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermissionRequest {
    @NotBlank(message = "权限名称不能为空")
    private String permissionName;
    @NotBlank(message = "权限标识不能为空")
    private String permissionKey;
    private Long parentId = 0L;
    private Integer type = 1;
    private String path;
    private String icon;
    private Integer sortOrder = 0;
}

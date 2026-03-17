package com.product.app.dto;

import lombok.Data;

@Data
public class RoleRequest {
    private String roleName;
    private String roleKey;
    private String remark;
    private Integer status;
}

package com.auth.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_product")
public class SysProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productName;
    private String productKey;
    private String productSecret;
    private String description;
    private String homepageUrl;
    private String redirectUris;
    private String logoUrl;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

package com.auth.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统产品实体类
 * <p>对应数据库表 sys_product，是对 Spring Authorization Server 注册客户端的业务层封装。
 * 每个产品对应 oauth2_registered_client 表中的一条客户端记录，
 * 两表通过 productKey（即 client_id）字段关联。</p>
 */
@Data
@TableName("sys_product")
public class SysProduct {
    /** 产品主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 产品名称，对应 oauth2_registered_client.client_name */
    private String productName;
    /** 产品唯一标识符（即 OAuth2 client_id），格式为 "app-" + 8 位随机字符 */
    private String productKey;
    /** 产品 Client Secret 明文，仅在管理后台展示，不参与 OAuth2 认证流程 */
    private String productSecret;
    /** 产品描述信息 */
    private String description;
    /** 产品首页地址 */
    private String homepageUrl;
    /** OAuth2 授权码回调地址列表，以 JSON 数组字符串格式存储 */
    private String redirectUris;
    /** 产品 Logo 图片地址 */
    private String logoUrl;
    /** 产品状态：1=启用，0=禁用；禁用时 OAuth2 授权请求将被拒绝 */
    private Integer status;
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

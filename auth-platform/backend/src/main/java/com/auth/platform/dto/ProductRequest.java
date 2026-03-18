package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 产品创建/更新请求数据传输对象
 * <p>封装创建或更新 OAuth2 应用产品时前端提交的数据。
 * 产品对应 Spring Authorization Server 中的 RegisteredClient（注册客户端）。</p>
 */
@Data
public class ProductRequest {
    /** 产品名称，不能为空，将同步更新到 oauth2_registered_client.client_name */
    @NotBlank(message = "产品名称不能为空")
    private String productName;
    /** 产品描述信息，可为空 */
    private String description;
    /** 产品首页地址，可为空 */
    private String homepageUrl;
    /** OAuth2 授权码回调地址列表，将序列化后存入数据库 */
    private List<String> redirectUris;
    /** 产品 Logo 图片地址，可为空 */
    private String logoUrl;
}

package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ProductRequest {
    @NotBlank(message = "产品名称不能为空")
    private String productName;
    private String description;
    private String homepageUrl;
    private List<String> redirectUris;
    private String logoUrl;
}

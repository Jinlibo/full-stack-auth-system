package com.auth.platform.controller;

import com.auth.platform.entity.SysProduct;
import com.auth.platform.mapper.SysProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Public endpoint for the frontend OAuth consent/login pages to query
 * client application info (name, logo, enabled status).
 * Mapped under /api/oauth2/** which is in the permitAll list.
 */
@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2ClientInfoController {

    private final SysProductMapper productMapper;

    @GetMapping("/client-info")
    public ResponseEntity<Map<String, Object>> clientInfo(@RequestParam String clientId) {
        SysProduct product = productMapper.selectOne(
                new LambdaQueryWrapper<SysProduct>().eq(SysProduct::getProductKey, clientId));

        Map<String, Object> info = new HashMap<>();
        if (product == null) {
            info.put("found", false);
            info.put("enabled", false);
        } else {
            info.put("found", true);
            info.put("enabled", product.getStatus() == 1);
            info.put("name", product.getProductName());
            info.put("description", product.getDescription());
            info.put("logoUrl", product.getLogoUrl());
            info.put("homepageUrl", product.getHomepageUrl());
        }
        return ResponseEntity.ok(info);
    }
}

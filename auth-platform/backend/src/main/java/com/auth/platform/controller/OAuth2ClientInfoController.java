package com.auth.platform.controller;

import com.auth.platform.entity.SysProduct;
import com.auth.platform.mapper.SysProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Public endpoint for the frontend OAuth consent/login pages to query
 * client application info (name, logo, enabled status).
 * Mapped under /api/oauth2/** which is in the permitAll list.
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2ClientInfoController {

    private final SysProductMapper productMapper;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationConsentService consentService;
    private final PasswordEncoder passwordEncoder;

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

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/revoke-consent")
    public ResponseEntity<Map<String, Object>> revokeConsent(@RequestBody Map<String, String> body) {
        // 从安全上下文获取当前已认证用户，不信任请求体中的 username
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");

        Map<String, Object> result = new HashMap<>();

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            result.put("success", false);
            result.put("message", "客户端不存在");
            return ResponseEntity.badRequest().body(result);
        }

        if (!passwordEncoder.matches(clientSecret, registeredClient.getClientSecret())) {
            result.put("success", false);
            result.put("message", "客户端密钥验证失败");
            return ResponseEntity.badRequest().body(result);
        }

        OAuth2AuthorizationConsent consent = consentService.findById(registeredClient.getId(), username);
        if (consent != null) {
            consentService.remove(consent);
            log.info("已撤销用户 [{}] 对客户端 [{}] 的授权同意", username, clientId);
        }

        result.put("success", true);
        return ResponseEntity.ok(result);
    }
}

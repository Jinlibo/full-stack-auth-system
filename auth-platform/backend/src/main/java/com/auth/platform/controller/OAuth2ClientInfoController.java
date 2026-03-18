package com.auth.platform.controller;

import com.auth.platform.entity.SysProduct;
import com.auth.platform.mapper.SysProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 客户端信息查询与授权同意管理控制器
 *
 * <p>提供供前端 OAuth2 授权/登录页面使用的公开接口，无需用户认证即可访问
 * （对应路径 /api/oauth2/** 已在 SecurityConfig 的 permitAll 白名单中）。
 *
 * <p>主要功能：
 * <ul>
 *   <li>GET /api/oauth2/client-info：前端 OAuth 同意页根据 clientId 查询应用名称、Logo、状态等展示信息</li>
 *   <li>DELETE /api/oauth2/revoke-consent：供第三方应用后端调用，撤销指定用户对该应用的授权同意记录</li>
 * </ul>
 *
 * @author auth-platform
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2ClientInfoController {

    /** 产品 Mapper，用于根据 clientId（productKey）查询产品信息 */
    private final SysProductMapper productMapper;

    /** 已注册客户端仓库，用于校验客户端是否存在 */
    private final RegisteredClientRepository registeredClientRepository;

    /** OAuth2 授权同意服务，用于查询和撤销授权同意记录 */
    private final OAuth2AuthorizationConsentService consentService;

    /** 密码编码器，用于校验请求中的 clientSecret 与数据库中存储的 BCrypt 哈希 */
    private final PasswordEncoder passwordEncoder;

    /**
     * 查询 OAuth2 客户端（产品）的展示信息
     *
     * <p>GET /api/oauth2/client-info?clientId=xxx
     * 前端 OAuth2 授权确认页使用此接口获取应用名称、Logo、描述等展示给用户。
     * 若产品不存在或已禁用，前端应拒绝继续授权流程。
     *
     * @param clientId OAuth2 Client ID（即 sys_product 表的 product_key 字段）
     * @return 包含 found（是否存在）、enabled（是否启用）、name、description、logoUrl、homepageUrl 等字段的 Map
     */
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

    /**
     * 撤销用户对指定 OAuth2 客户端的授权同意
     *
     * <p>DELETE /api/oauth2/revoke-consent
     * 此接口用于服务器间调用（如 product-app 后端调用 auth-platform）：
     * 调用方通过请求体中的 clientSecret 证明自身身份，指定 username 作为要解绑的目标用户。
     *
     * <p>请求体示例：
     * <pre>
     * {
     *   "username": "zhangsan",
     *   "clientId": "product-app",
     *   "clientSecret": "admin123"
     * }
     * </pre>
     *
     * <p>若目标用户对该客户端的授权同意记录不存在，接口也返回成功（幂等操作）。
     *
     * @param body 包含 username、clientId、clientSecret 的请求体 Map
     * @return 包含 success 字段的响应，失败时额外包含 message 字段
     */
    @DeleteMapping("/revoke-consent")
    public ResponseEntity<Map<String, Object>> revokeConsent(@RequestBody Map<String, String> body) {
        // 服务器间调用（product-app 后端 → auth-platform）：
        // 调用方通过 clientSecret 证明自身身份，username 由调用方指定（解绑的目标用户）
        String username = body.get("username");
        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");

        Map<String, Object> result = new HashMap<>();

        if (username == null || username.isBlank()) {
            result.put("success", false);
            result.put("message", "username 不能为空");
            return ResponseEntity.badRequest().body(result);
        }

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

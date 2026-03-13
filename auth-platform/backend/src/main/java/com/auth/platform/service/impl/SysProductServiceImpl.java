package com.auth.platform.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.auth.platform.common.BusinessException;
import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.ProductRequest;
import com.auth.platform.entity.SysProduct;
import com.auth.platform.mapper.SysProductMapper;
import com.auth.platform.service.SysProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SysProductServiceImpl extends ServiceImpl<SysProductMapper, SysProduct>
        implements SysProductService {

    private final SysProductMapper productMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<SysProduct> pageProducts(PageQuery query) {
        Page<SysProduct> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(SysProduct::getProductName, query.getKeyword())
                    .or().like(SysProduct::getProductKey, query.getKeyword());
        }
        wrapper.orderByDesc(SysProduct::getCreatedAt);
        return productMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void createProduct(ProductRequest request) {
        String clientId = "app-" + RandomUtil.randomString(8);
        String rawSecret = IdUtil.fastSimpleUUID();

        SysProduct product = new SysProduct();
        product.setProductName(request.getProductName());
        product.setProductKey(clientId);
        product.setProductSecret(rawSecret); // 存原始密钥,方便查看
        product.setDescription(request.getDescription());
        product.setHomepageUrl(request.getHomepageUrl());
        product.setLogoUrl(request.getLogoUrl());
        product.setStatus(1);

        String redirectUrisJson = "[]";
        if (request.getRedirectUris() != null && !request.getRedirectUris().isEmpty()) {
            try {
                redirectUrisJson = objectMapper.writeValueAsString(request.getRedirectUris());
            } catch (JsonProcessingException e) {
                throw new BusinessException("回调URI格式错误");
            }
        }
        product.setRedirectUris(redirectUrisJson);
        productMapper.insert(product);

        // 同步到oauth2_registered_client
        String redirectUriStr = String.join(",", request.getRedirectUris() != null ? request.getRedirectUris() : java.util.List.of());
        String encodedSecret = passwordEncoder.encode(rawSecret);
        String sql = "INSERT INTO oauth2_registered_client " +
                "(id, client_id, client_secret, client_name, client_authentication_methods, " +
                "authorization_grant_types, redirect_uris, scopes, client_settings, token_settings) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                UUID.randomUUID().toString(), clientId, encodedSecret, request.getProductName(),
                "client_secret_basic,client_secret_post",
                "authorization_code,refresh_token",
                redirectUriStr,
                "openid,profile,email",
                "{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-proof-key\":false,\"settings.client.require-authorization-consent\":true}",
                "{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":true,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",3600.000000000],\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",86400.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300.000000000]}"
        );
    }

    @Override
    @Transactional
    public void updateProduct(Long id, ProductRequest request) {
        SysProduct product = productMapper.selectById(id);
        if (product == null) throw new BusinessException(404, "产品不存在");

        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setHomepageUrl(request.getHomepageUrl());
        product.setLogoUrl(request.getLogoUrl());
        if (request.getRedirectUris() != null) {
            try {
                product.setRedirectUris(objectMapper.writeValueAsString(request.getRedirectUris()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("回调URI格式错误");
            }
            // 同步更新oauth2_registered_client
            String redirectUriStr = String.join(",", request.getRedirectUris());
            jdbcTemplate.update("UPDATE oauth2_registered_client SET redirect_uris = ?, client_name = ? WHERE client_id = ?",
                    redirectUriStr, request.getProductName(), product.getProductKey());
        }
        productMapper.updateById(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        SysProduct product = productMapper.selectById(id);
        if (product == null) return;
        productMapper.deleteById(id);
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", product.getProductKey());
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        SysProduct product = productMapper.selectById(id);
        if (product == null) throw new BusinessException(404, "产品不存在");
        product.setStatus(product.getStatus() == 1 ? 0 : 1);
        productMapper.updateById(product);
    }
}

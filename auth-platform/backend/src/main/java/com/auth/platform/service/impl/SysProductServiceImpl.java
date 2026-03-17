package com.auth.platform.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.auth.platform.common.BusinessException;
import com.auth.platform.common.OAuth2ClientDefaults;
import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.ProductRequest;
import com.auth.platform.entity.SysProduct;
import com.auth.platform.mapper.OAuth2RegisteredClientMapper;
import com.auth.platform.mapper.SysProductMapper;
import com.auth.platform.service.SysProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 产品管理服务实现（SysProductService Implementation）
 *
 * <p>产品（Product）是对 Spring Authorization Server 注册客户端（RegisteredClient）的业务层抽象，
 * 每个产品对应 oauth2_registered_client 表中的一条客户端注册记录。
 *
 * <p>数据双写机制（sys_product + oauth2_registered_client）：
 * <ul>
 *   <li>sys_product：业务产品表，存储产品名称、描述、首页地址，以及明文 Secret（方便管理后台展示）</li>
 *   <li>oauth2_registered_client：Spring Authorization Server 的客户端注册表，
 *       存储 BCrypt 加密的 Secret（Spring Auth Server 在认证流程中查此表验证客户端身份）</li>
 *   <li>两表通过 productKey（=client_id）字段关联</li>
 * </ul>
 *
 * <p>为何使用专用 Mapper 而非 MyBatis-Plus 操作 oauth2_registered_client？
 * <ul>
 *   <li>oauth2_registered_client 是 Spring Authorization Server 的内置表，
 *       其结构（尤其是 client_settings 和 token_settings 的 JSON 格式）比较特殊</li>
 *   <li>该表无 deleted、created_at 等 MP 自动填充字段，使用原生 MyBatis 注解 Mapper 直接写 SQL</li>
 * </ul>
 *
 * @author auth-platform
 */
@Service
@RequiredArgsConstructor
public class SysProductServiceImpl extends ServiceImpl<SysProductMapper, SysProduct>
        implements SysProductService {

    /**
     * 产品 Mapper，操作 sys_product 表
     */
    private final SysProductMapper productMapper;

    /**
     * BCrypt 密码加密器
     * 用于将 Client Secret 加密后存入 oauth2_registered_client 表
     * （Spring Authorization Server 在验证 client_secret_basic 时使用 BCrypt 比对）
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * MyBatis Mapper，操作 oauth2_registered_client 表
     * 使用原生 SQL 注解，避免 MyBatis-Plus 自动填充对该特殊表的干扰
     */
    private final OAuth2RegisteredClientMapper oauth2ClientMapper;

    /**
     * Jackson ObjectMapper，用于将 redirectUris 字符串数组序列化为 JSON 字符串
     * 以 JSON 格式存储在 sys_product.redirect_uris 字段中
     */
    private final ObjectMapper objectMapper;

    /**
     * 分页查询产品列表
     *
     * <p>支持按产品名称（productName）和 Client ID（productKey）关键字模糊搜索。
     * 按 created_at 倒序排列，最新创建的产品显示在最前面。
     *
     * @param query 分页查询参数（pageNum、pageSize、keyword）
     * @return 分页产品列表
     */
    @Override
    public IPage<SysProduct> pageProducts(PageQuery query) {
        Page<SysProduct> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            // 在产品名称和 Client ID 两个字段中模糊搜索（OR 关系）
            wrapper.like(SysProduct::getProductName, query.getKeyword())
                    .or().like(SysProduct::getProductKey, query.getKeyword());
        }
        wrapper.orderByDesc(SysProduct::getCreatedAt);
        return productMapper.selectPage(page, wrapper);
    }

    /**
     * 创建产品（同时注册 OAuth2 客户端）
     *
     * <p>完整流程：
     * <ol>
     *   <li>自动生成 Client ID（productKey）：格式 "app-" + 8位随机小写字母数字</li>
     *   <li>自动生成 Client Secret（rawSecret）：UUID 格式，32位十六进制字符串（去除短横线）</li>
     *   <li>将产品信息和明文 Secret 存入 sys_product 表</li>
     *   <li>将 BCrypt 加密后的 Secret 和客户端配置存入 oauth2_registered_client 表</li>
     * </ol>
     *
     * <p>OAuth2 客户端配置说明（写入 oauth2_registered_client 的固定值）：
     * <ul>
     *   <li>client_authentication_methods：支持 client_secret_basic 和 client_secret_post</li>
     *   <li>authorization_grant_types：支持 authorization_code 和 refresh_token</li>
     *   <li>scopes：openid、profile、email（OIDC 标准 scope）</li>
     *   <li>require-proof-key：false（不强制要求 PKCE）</li>
     *   <li>require-authorization-consent：true（需要用户在授权页手动确认）</li>
     *   <li>access-token-time-to-live：3600 秒（1 小时）</li>
     *   <li>refresh-token-time-to-live：86400 秒（24 小时）</li>
     * </ul>
     *
     * <p>{@code @Transactional} 确保 sys_product 写入和 oauth2_registered_client 写入的原子性：
     * 若 JDBC 操作失败，事务回滚，sys_product 的插入也会撤销。
     *
     * @param request 产品创建请求（productName、description、homepageUrl、redirectUris）
     */
    @Override
    @Transactional
    public void createProduct(ProductRequest request) {
        // 第一步：自动生成唯一的 Client ID（"app-" + 8 位随机小写字母数字）
        String clientId = "app-" + RandomUtil.randomString(8);

        // 第二步：自动生成 Client Secret（Hutool 的 fastSimpleUUID 生成无横线的 UUID）
        String rawSecret = IdUtil.fastSimpleUUID();

        // 第三步：构建并插入 sys_product 记录
        SysProduct product = new SysProduct();
        product.setProductName(request.getProductName());
        product.setProductKey(clientId);     // 存储 Client ID
        product.setProductSecret(rawSecret); // 存储明文 Secret（方便管理后台展示给用户）
        product.setDescription(request.getDescription());
        product.setHomepageUrl(request.getHomepageUrl());
        product.setLogoUrl(request.getLogoUrl());
        product.setStatus(1); // 默认启用

        // 将 redirectUris 数组序列化为 JSON 字符串（sys_product 用），同时生成逗号分隔字符串（oauth2 表用）
        List<String> uris = (request.getRedirectUris() != null) ? request.getRedirectUris() : java.util.List.of();
        String redirectUrisJson;
        try {
            redirectUrisJson = objectMapper.writeValueAsString(uris);
        } catch (JsonProcessingException e) {
            throw new BusinessException("回调 URI 格式错误，无法序列化");
        }
        String redirectUriStr = String.join(",", uris);

        product.setRedirectUris(redirectUrisJson);
        productMapper.insert(product);

        // 第四步：同步注册到 oauth2_registered_client 表

        // 使用 BCrypt 加密 Client Secret（Spring Auth Server 在认证时会用 BCrypt.matches 比对）
        String encodedSecret = passwordEncoder.encode(rawSecret);

        oauth2ClientMapper.insert(
                UUID.randomUUID().toString(),
                clientId,
                encodedSecret,
                request.getProductName(),
                OAuth2ClientDefaults.CLIENT_AUTH_METHODS,
                OAuth2ClientDefaults.GRANT_TYPES,
                redirectUriStr,
                OAuth2ClientDefaults.SCOPES,
                OAuth2ClientDefaults.CLIENT_SETTINGS,
                OAuth2ClientDefaults.TOKEN_SETTINGS
        );
    }

    /**
     * 更新产品信息（同步更新 OAuth2 客户端）
     *
     * <p>可更新的字段：productName、description、homepageUrl、logoUrl、redirectUris
     * Client ID（productKey）不允许修改，修改会导致 OAuth2 认证流程中的客户端验证失败。
     *
     * <p>同步逻辑：若 redirectUris 发生变化，同步更新 oauth2_registered_client 表的：
     * <ul>
     *   <li>redirect_uris：新的回调地址列表</li>
     *   <li>client_name：产品名称变化时一并同步</li>
     * </ul>
     *
     * @param id      要更新的产品 ID
     * @param request 更新请求
     * @throws BusinessException 产品不存在时抛出 404
     */
    @Override
    @Transactional
    public void updateProduct(Long id, ProductRequest request) {
        SysProduct product = productMapper.selectById(id);
        if (product == null) throw new BusinessException(404, "产品不存在");

        // 更新 sys_product 基础字段
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setHomepageUrl(request.getHomepageUrl());
        product.setLogoUrl(request.getLogoUrl());

        // 若请求中包含 redirectUris，则更新（并同步到 oauth2_registered_client）
        if (request.getRedirectUris() != null) {
            try {
                // 将 List<String> 序列化为 JSON 字符串存储到 sys_product
                product.setRedirectUris(objectMapper.writeValueAsString(request.getRedirectUris()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("回调 URI 序列化失败");
            }

            // 同步更新 oauth2_registered_client 表中的 redirect_uris 和 client_name
            // 使用 productKey（Client ID）作为关联条件
            String redirectUriStr = String.join(",", request.getRedirectUris());
            oauth2ClientMapper.updateRedirectUris(
                    redirectUriStr,
                    request.getProductName(),  // 同步更新客户端名称
                    product.getProductKey()    // 按 client_id 定位记录
            );
        }

        productMapper.updateById(product);
    }

    /**
     * 删除产品（同步注销 OAuth2 客户端）
     *
     * <p>删除操作：
     * <ol>
     *   <li>从 sys_product 表删除产品记录</li>
     *   <li>从 oauth2_registered_client 表删除对应的客户端注册（按 client_id 匹配）</li>
     * </ol>
     * 客户端注销后，使用该 Client ID 发起的 OAuth2 授权请求将被 Spring Auth Server 拒绝
     * （错误：invalid_client）。
     *
     * <p>若产品不存在，静默返回（不抛异常），保证删除操作的幂等性。
     *
     * @param id 要删除的产品 ID
     */
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        SysProduct product = productMapper.selectById(id);
        // 若产品已不存在，静默返回（幂等处理，防止重复删除报错）
        if (product == null) return;

        // 删除 sys_product 记录
        productMapper.deleteById(id);

        // 按 client_id 从 oauth2_registered_client 表删除对应客户端
        oauth2ClientMapper.deleteByClientId(product.getProductKey());
    }

    /**
     * 切换产品启用/禁用状态（Toggle）
     *
     * <p>将当前 status 取反：1（启用）→ 0（禁用），0（禁用）→ 1（启用）。
     *
     * <p>当前实现仅修改 sys_product.status 字段，不影响 Spring Authorization Server 的授权流程。
     * 若要真正限制已禁用产品的 OAuth2 授权，需扩展 Spring Auth Server 的客户端查询逻辑，
     * 在查询客户端时联表检查 sys_product.status。
     *
     * @param id 产品 ID
     * @throws BusinessException 产品不存在时抛出 404
     */
    @Override
    @Transactional
    public void toggleStatus(Long id) {
        SysProduct product = productMapper.selectById(id);
        if (product == null) throw new BusinessException(404, "产品不存在");

        // 三元运算符实现状态切换：1 → 0，其他值 → 1
        product.setStatus(product.getStatus() == 1 ? 0 : 1);
        productMapper.updateById(product);
    }
}

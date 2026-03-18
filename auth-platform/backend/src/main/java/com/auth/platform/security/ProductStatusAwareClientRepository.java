package com.auth.platform.security;

import com.auth.platform.entity.SysProduct;
import com.auth.platform.mapper.SysProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * 带产品状态检查的 OAuth2 客户端仓库
 * <p>对 JDBC 实现的 {@link RegisteredClientRepository} 进行装饰（Decorator 模式），
 * 在返回客户端注册信息前，额外检查 sys_product 表中对应产品的启用状态。
 * 若产品已通过管理后台禁用，则该 client_id 的所有 OAuth2 授权请求将被拒绝，
 * Spring Authorization Server 会返回 "invalid_client" 错误。</p>
 */
public class ProductStatusAwareClientRepository implements RegisteredClientRepository {

    /** 被代理的原始客户端仓库（JDBC 实现，读取 oauth2_registered_client 表） */
    private final RegisteredClientRepository delegate;
    /** 产品 Mapper，用于查询 sys_product 表中的产品状态 */
    private final SysProductMapper productMapper;

    /**
     * 构造带产品状态检查的客户端仓库
     *
     * @param delegate      原始 JDBC 客户端仓库
     * @param productMapper 产品 Mapper
     */
    public ProductStatusAwareClientRepository(RegisteredClientRepository delegate,
                                              SysProductMapper productMapper) {
        this.delegate = delegate;
        this.productMapper = productMapper;
    }

    /**
     * 保存客户端注册信息（直接委托给原始仓库）
     *
     * @param registeredClient 要保存的客户端注册信息
     */
    @Override
    public void save(RegisteredClient registeredClient) {
        delegate.save(registeredClient);
    }

    /**
     * 根据记录 ID 查询客户端注册信息（附带产品状态检查）
     *
     * @param id 客户端记录 ID
     * @return 若客户端存在且对应产品已启用则返回客户端信息，否则返回 null
     */
    @Override
    public RegisteredClient findById(String id) {
        RegisteredClient client = delegate.findById(id);
        if (client == null) return null;
        return isEnabled(client.getClientId()) ? client : null;
    }

    /**
     * 根据 client_id 查询客户端注册信息（附带产品状态检查）
     *
     * @param clientId OAuth2 client_id
     * @return 若客户端存在且对应产品已启用则返回客户端信息，否则返回 null
     */
    @Override
    public RegisteredClient findByClientId(String clientId) {
        RegisteredClient client = delegate.findByClientId(clientId);
        if (client == null) return null;
        return isEnabled(clientId) ? client : null;
    }

    /**
     * 检查指定 client_id 对应的产品是否处于启用状态
     * <p>若 sys_product 中不存在对应记录（如手动插入的客户端），则默认允许通过。</p>
     *
     * @param clientId OAuth2 client_id（对应 sys_product.product_key）
     * @return true 表示产品已启用或不存在对应产品记录；false 表示产品已禁用
     */
    private boolean isEnabled(String clientId) {
        SysProduct product = productMapper.selectOne(
                new LambdaQueryWrapper<SysProduct>().eq(SysProduct::getProductKey, clientId));
        // 若 sys_product 中不存在对应记录（如手动插入的客户端），默认允许通过
        if (product == null) return true;
        return product.getStatus() == 1;
    }
}

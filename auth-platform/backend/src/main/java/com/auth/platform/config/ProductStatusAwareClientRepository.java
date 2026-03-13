package com.auth.platform.config;

import com.auth.platform.entity.SysProduct;
import com.auth.platform.mapper.SysProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Wraps the JDBC-backed RegisteredClientRepository and checks the product's
 * status in sys_product before returning the client. If a product has been
 * disabled via the admin dashboard, all OAuth2 authorization requests for
 * that client_id are rejected with "invalid_client".
 */
public class ProductStatusAwareClientRepository implements RegisteredClientRepository {

    private final RegisteredClientRepository delegate;
    private final SysProductMapper productMapper;

    public ProductStatusAwareClientRepository(RegisteredClientRepository delegate,
                                              SysProductMapper productMapper) {
        this.delegate = delegate;
        this.productMapper = productMapper;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        delegate.save(registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        RegisteredClient client = delegate.findById(id);
        if (client == null) return null;
        return isEnabled(client.getClientId()) ? client : null;
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        RegisteredClient client = delegate.findByClientId(clientId);
        if (client == null) return null;
        return isEnabled(clientId) ? client : null;
    }

    private boolean isEnabled(String clientId) {
        SysProduct product = productMapper.selectOne(
                new LambdaQueryWrapper<SysProduct>().eq(SysProduct::getProductKey, clientId));
        // If no corresponding sys_product record, allow (e.g. manually inserted clients)
        if (product == null) return true;
        return product.getStatus() == 1;
    }
}

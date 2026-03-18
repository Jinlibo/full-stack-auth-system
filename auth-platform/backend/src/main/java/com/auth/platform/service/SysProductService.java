package com.auth.platform.service;

import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.ProductRequest;
import com.auth.platform.entity.SysProduct;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统产品管理服务接口
 * <p>继承 MyBatis-Plus 的 {@link IService}，获得基础 CRUD 能力。
 * 产品对应 OAuth2 注册客户端，所有写操作均同步更新 oauth2_registered_client 表。</p>
 */
public interface SysProductService extends IService<SysProduct> {

    /**
     * 分页查询产品列表
     * <p>支持按产品名称和 Client ID 关键字模糊搜索，按创建时间倒序排列。</p>
     *
     * @param query 分页查询参数（pageNum、pageSize、keyword）
     * @return 分页产品列表
     */
    IPage<SysProduct> pageProducts(PageQuery query);

    /**
     * 创建产品（同时注册 OAuth2 客户端）
     * <p>自动生成 Client ID 和 Client Secret，同步写入 sys_product 和 oauth2_registered_client 两表。</p>
     *
     * @param request 产品创建请求（productName、description、homepageUrl、redirectUris 等）
     */
    void createProduct(ProductRequest request);

    /**
     * 更新产品信息（同步更新 OAuth2 客户端）
     * <p>可更新产品名称、描述、首页地址、回调地址列表等，产品不存在时抛出 404 异常。</p>
     *
     * @param id      要更新的产品 ID
     * @param request 更新请求
     */
    void updateProduct(Long id, ProductRequest request);

    /**
     * 删除产品（同步注销 OAuth2 客户端）
     * <p>从 sys_product 和 oauth2_registered_client 两表中删除记录，删除后该 client_id 的授权请求将被拒绝。
     * 若产品不存在，静默返回（幂等处理）。</p>
     *
     * @param id 要删除的产品 ID
     */
    void deleteProduct(Long id);

    /**
     * 切换产品启用/禁用状态
     * <p>将当前状态取反：1（启用）→ 0（禁用），0（禁用）→ 1（启用）。
     * 产品不存在时抛出 404 异常。</p>
     *
     * @param id 产品 ID
     */
    void toggleStatus(Long id);
}

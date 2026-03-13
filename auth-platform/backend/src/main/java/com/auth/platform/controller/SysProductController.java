package com.auth.platform.controller;

import com.auth.platform.common.PageQuery;
import com.auth.platform.common.R;
import com.auth.platform.dto.ProductRequest;
import com.auth.platform.entity.SysProduct;
import com.auth.platform.service.SysProductService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 产品管理控制器（SysProduct Controller）
 *
 * <p>"产品"（Product）是本系统对 OAuth2 注册客户端（RegisteredClient）的业务封装。
 * 每个产品代表一个接入 OAuth2 授权服务的第三方应用，拥有：
 * <ul>
 *   <li>productKey（Client ID）：自动生成，格式为 "app-" + 8位随机字母数字</li>
 *   <li>productSecret（Client Secret）：自动生成的 UUID，明文存储于 sys_product 表方便展示</li>
 *   <li>同时在 oauth2_registered_client 表中存储 BCrypt 加密后的 Secret，供 Spring Authorization Server 使用</li>
 * </ul>
 *
 * <p>基础路径：/api/products
 *
 * <p>接口权限控制：
 * <ul>
 *   <li>列表/详情：product:list</li>
 *   <li>新增：product:add</li>
 *   <li>编辑/切换状态：product:edit</li>
 *   <li>删除：product:delete</li>
 * </ul>
 *
 * @author auth-platform
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class SysProductController {

    /**
     * 产品管理服务，包含产品 CRUD 和 OAuth2 客户端同步逻辑
     */
    private final SysProductService productService;

    /**
     * 分页查询产品列表
     *
     * <p>GET /api/products?pageNum=1&pageSize=10&keyword=xxx
     * 支持按产品名称（productName）和 Client ID（productKey）关键字搜索
     *
     * @param query 分页查询参数（pageNum、pageSize、keyword）
     * @return 分页产品数据
     */
    @GetMapping
    @PreAuthorize("hasAuthority('product:list')")
    public R<IPage<SysProduct>> pageProducts(PageQuery query) {
        return R.ok(productService.pageProducts(query));
    }

    /**
     * 根据 ID 查询单个产品详情
     *
     * <p>GET /api/products/{id}
     *
     * @param id 产品 ID（路径变量）
     * @return 产品详情（含 productKey、productSecret 等字段）
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:list')")
    public R<SysProduct> getProduct(@PathVariable Long id) {
        return R.ok(productService.getById(id));
    }

    /**
     * 创建新产品（同步注册 OAuth2 客户端）
     *
     * <p>POST /api/products
     * 请求体：{
     * productName:   产品名称（必填）
     * description:   产品描述（可选）
     * homepageUrl:   产品首页 URL（可选，显示在 OAuth2 授权确认页）
     * redirectUris:  允许的回调 URI 列表（字符串数组，如 ["https://app.com/callback"]）
     * }
     *
     * <p>后端自动生成：Client ID（app-xxxxxxxx）和 Client Secret（UUID 明文）。
     * 同时在 oauth2_registered_client 表中注册客户端（BCrypt 加密 Secret）。
     *
     * @param request 产品创建请求，@Valid 触发参数校验
     * @return 无 data 的成功响应（productKey 和 productSecret 需重新查询产品列表获取）
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:add')")
    public R<Void> createProduct(@Valid @RequestBody ProductRequest request) {
        productService.createProduct(request);
        return R.ok();
    }

    /**
     * 更新产品信息（同步更新 OAuth2 客户端）
     *
     * <p>PUT /api/products/{id}
     * Client ID（productKey）不允许修改，其余字段均可更新。
     * 若 redirectUris 变化，会同步更新 oauth2_registered_client 表中的 redirect_uris 字段。
     *
     * @param id      要更新的产品 ID（路径变量）
     * @param request 产品更新请求
     * @return 无 data 的成功响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:edit')")
    public R<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        productService.updateProduct(id, request);
        return R.ok();
    }

    /**
     * 删除产品（同步注销 OAuth2 客户端）
     *
     * <p>DELETE /api/products/{id}
     * 删除操作会同时：
     * <ol>
     *   <li>从 sys_product 表删除产品记录</li>
     *   <li>从 oauth2_registered_client 表删除对应的客户端注册（按 client_id 匹配）</li>
     * </ol>
     * 客户端被注销后，使用该 Client ID 发起的 OAuth2 授权请求将被 Spring Authorization Server 拒绝。
     *
     * @param id 要删除的产品 ID（路径变量）
     * @return 无 data 的成功响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public R<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return R.ok();
    }

    /**
     * 切换产品启用/禁用状态
     *
     * <p>PUT /api/products/{id}/toggle-status
     * 无请求体，后端自动将当前状态取反（1→0 或 0→1）。
     *
     * <p>当前仅修改 sys_product.status 字段，不影响 OAuth2 授权流程。
     * 若需要真正禁止 OAuth2 授权，可在 Spring Authorization Server 的客户端查询逻辑中
     * 增加对 sys_product.status 的检查。
     *
     * @param id 产品 ID（路径变量）
     * @return 无 data 的成功响应
     */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('product:edit')")
    public R<Void> toggleStatus(@PathVariable Long id) {
        productService.toggleStatus(id);
        return R.ok();
    }
}

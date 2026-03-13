/**
 * 产品管理 API 模块
 *
 * "产品"（Product）是本系统对 OAuth2 注册客户端（RegisteredClient）的业务抽象。
 * 每个产品对应一个接入 OAuth2 授权服务的第三方应用，拥有唯一的 Client ID 和 Client Secret。
 *
 * 数据同步关系：
 *  - sys_product 表：存储产品业务信息（含明文 Secret，方便在管理后台展示）
 *  - oauth2_registered_client 表：Spring Authorization Server 的客户端注册表
 *    （存储 BCrypt 加密后的 Secret，OAuth2 流程中使用）
 *  两表通过 productKey（即 client_id）字段关联，新增/更新/删除时需同步操作
 *
 * 接口权限要求：
 *  - getProducts / getProduct：product:list
 *  - createProduct：product:add
 *  - updateProduct / toggleProductStatus：product:edit
 *  - deleteProduct：product:delete
 */

import request from '../utils/request'

/**
 * 分页查询产品列表
 *
 * 请求：GET /api/products
 * 参数：{
 *   pageNum:   当前页码
 *   pageSize:  每页条数
 *   keyword:   关键字（在 productName 和 productKey 中模糊搜索）
 * }
 * 响应：R<IPage<SysProduct>>
 *   - records：产品列表
 *   - total：总记录数
 *
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export const getProducts = (params) => request.get('/products', {params})

/**
 * 根据 ID 获取单个产品详情
 *
 * 请求：GET /api/products/{id}
 * 响应：R<SysProduct>
 *
 * @param {number} id - 产品 ID
 * @returns {Promise}
 */
export const getProduct = (id) => request.get(`/products/${id}`)

/**
 * 创建新产品（注册 OAuth2 客户端）
 *
 * 请求：POST /api/products
 * 请求体：{
 *   productName:   产品名称
 *   description:   产品描述（可选）
 *   homepageUrl:   产品首页地址（可选，显示在 OAuth2 授权确认页）
 *   redirectUris:  允许的回调 URI 列表（字符串数组，如 ["https://app.com/callback"]）
 * }
 * 响应：R<Void>
 *
 * 后端自动生成：
 *  - productKey（Client ID）：格式为 "app-" + 8位随机字母数字
 *  - productSecret（Client Secret）：UUID 格式明文（sys_product 表存明文）
 *  - 同时将 BCrypt 加密的 Secret 写入 oauth2_registered_client 表
 *
 * @param {Object} data - 产品创建数据
 * @returns {Promise}
 */
export const createProduct = (data) => request.post('/products', data)

/**
 * 更新产品信息
 *
 * 请求：PUT /api/products/{id}
 * 请求体：同 createProduct（不含 productKey，Client ID 不允许修改）
 * 响应：R<Void>
 *
 * 同步逻辑：
 *  - 若 redirectUris 有变化，同步更新 oauth2_registered_client 表中的 redirect_uris 字段
 *  - 同步更新 client_name 字段（产品名称）
 *
 * @param {number} id   - 产品 ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export const updateProduct = (id, data) => request.put(`/products/${id}`, data)

/**
 * 删除产品（注销 OAuth2 客户端）
 *
 * 请求：DELETE /api/products/{id}
 * 响应：R<Void>
 *
 * 后端行为：
 *  1. 从 sys_product 表删除记录
 *  2. 根据 productKey（Client ID）从 oauth2_registered_client 表删除对应的客户端注册
 *  客户端被注销后，使用该 Client ID 发起的 OAuth2 授权请求将被拒绝
 *
 * @param {number} id - 产品 ID
 * @returns {Promise}
 */
export const deleteProduct = (id) => request.delete(`/products/${id}`)

/**
 * 切换产品启用/禁用状态
 *
 * 请求：PUT /api/products/{id}/toggle-status
 * 响应：R<Void>
 *
 * 后端逻辑：若当前 status=1（启用），则改为 0（禁用），反之亦然（toggle 切换）
 *
 * 注意：目前仅切换 sys_product.status 字段，
 * 如需同步影响 OAuth2 授权流程，需后续集成 oauth2_registered_client 的客户端状态控制
 *
 * @param {number} id - 产品 ID
 * @returns {Promise}
 */
export const toggleProductStatus = (id) => request.put(`/products/${id}/toggle-status`)

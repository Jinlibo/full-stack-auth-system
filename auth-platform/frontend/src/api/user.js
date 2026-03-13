/**
 * 用户管理 API 模块
 *
 * 封装系统用户的 CRUD 接口，以及当前登录用户的个人信息和密码管理接口。
 *
 * 接口权限说明：
 *  - getUsers / getUser / createUser / updateUser / deleteUser：需要 system:user:* 权限
 *  - changePassword / updateProfile：当前登录用户自操作，无需额外权限
 */

import request from '../utils/request'

/**
 * 分页查询用户列表
 *
 * 请求：GET /api/users
 * 参数：
 *  - pageNum:   当前页码（从 1 开始）
 *  - pageSize:  每页条数（默认 10）
 *  - keyword:   搜索关键字（在 username、nickname、email 中模糊匹配）
 * 响应：R<IPage<UserInfo>>
 *   - records:  当前页的用户信息列表
 *   - total:    总记录数（用于分页器）
 *
 * @param {Object} params - 查询参数对象
 * @returns {Promise}
 */
export const getUsers = (params) => request.get('/users', {params})

/**
 * 根据 ID 获取单个用户信息
 *
 * 请求：GET /api/users/{id}
 * 响应：R<UserInfo>
 *
 * @param {number} id - 用户 ID
 * @returns {Promise}
 */
export const getUser = (id) => request.get(`/users/${id}`)

/**
 * 创建新用户
 *
 * 请求：POST /api/users
 * 请求体：{
 *   username:  用户名（唯一，不可重复）
 *   password:  原始密码（后端会 BCrypt 加密后存储）
 *   nickname:  昵称（可选，不传则默认使用 username）
 *   email:     邮箱（可选）
 *   phone:     手机号（可选）
 *   roleIds:   角色 ID 列表（可选，如 [1, 2]）
 * }
 * 响应：R<Void>
 *
 * @param {Object} data - 用户创建信息
 * @returns {Promise}
 */
export const createUser = (data) => request.post('/users', data)

/**
 * 更新用户信息
 *
 * 请求：PUT /api/users/{id}
 * 请求体：{
 *   nickname:  昵称
 *   email:     邮箱
 *   phone:     手机号
 *   avatar:    头像 URL
 *   status:    账号状态（1=正常，0=禁用）
 *   roleIds:   新的角色 ID 列表（全量替换，不传则保持不变）
 * }
 * 响应：R<Void>
 *
 * 注意：不允许通过此接口修改密码，密码修改请使用 changePassword 接口
 *
 * @param {number} id   - 用户 ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export const updateUser = (id, data) => request.put(`/users/${id}`, data)

/**
 * 删除用户
 *
 * 请求：DELETE /api/users/{id}
 * 响应：R<Void>
 *
 * 注意：
 *  - 后端会阻止删除 id=1 的超级管理员
 *  - 删除用户同时会删除其所有角色关联记录（sys_user_role 表）
 *
 * @param {number} id - 要删除的用户 ID
 * @returns {Promise}
 */
export const deleteUser = (id) => request.delete(`/users/${id}`)

/**
 * 修改当前登录用户的密码
 *
 * 请求：PUT /api/users/me/password
 * 请求体：{
 *   oldPassword:  旧密码（明文，后端用 BCrypt 校验）
 *   newPassword:  新密码（明文，后端 BCrypt 加密后存储）
 * }
 * 响应：R<Void>
 *
 * 安全性：
 *  - 需要提供旧密码进行身份验证，防止 token 被盗用后恶意改密
 *  - 后端使用 BCrypt 的 matches() 方法对比旧密码
 *
 * @param {Object} data - { oldPassword, newPassword }
 * @returns {Promise}
 */
export const changePassword = (data) => request.put('/users/me/password', data)

/**
 * 更新当前登录用户的个人资料
 *
 * 请求：PUT /api/users/me/profile
 * 请求体：{
 *   nickname:  昵称
 *   email:     邮箱
 *   phone:     手机号
 * }
 * 响应：R<Void>
 *
 * 注意：此接口仅允许修改非敏感字段，不涉及密码和角色
 *
 * @param {Object} data - 个人资料数据
 * @returns {Promise}
 */
export const updateProfile = (data) => request.put('/users/me/profile', data)

/**
 * 认证相关 API 模块
 *
 * 封装与用户认证流程相关的所有接口调用：
 *  - login:          用户名密码登录，返回 JWT Access Token + Refresh Token
 *  - register:       用户注册（会分配默认角色 USER）
 *  - logout:         退出登录（后端将 token 加入黑名单 + 从 Redis 删除缓存）
 *  - getCurrentUser: 获取当前登录用户的详细信息（用于路由守卫中懒加载用户信息）
 *
 * 所有函数返回 Promise<R<T>>，其中 R<T> 是后端统一响应结构：
 *   { code: number, message: string, data: T }
 */

import request from '../utils/request'

/**
 * 用户名密码登录
 *
 * 请求：POST /api/auth/login
 * 请求体：{ username: string, password: string }
 * 响应：R<LoginResponse>
 *   - accessToken:   JWT Access Token（短期有效，默认 2 小时）
 *   - refreshToken:  JWT Refresh Token（长期有效，默认 7 天）
 *   - expiresIn:     Access Token 有效期（秒）
 *   - userInfo:      用户基本信息（id、username、nickname、roles、permissions）
 *
 * @param {Object} data - 登录表单数据 { username, password }
 * @returns {Promise} 登录结果
 */
export const login = (data) => request.post('/auth/login', data)

/**
 * 用户注册
 *
 * 请求：POST /api/auth/register
 * 请求体：{ username, password, email?, nickname? }
 * 后端行为：
 *  1. 检查用户名是否已存在
 *  2. BCrypt 加密密码并保存用户
 *  3. 自动分配默认角色（id=3，普通用户）
 * 响应：R<Void>（无 data）
 *
 * @param {Object} data - 注册信息
 * @returns {Promise}
 */
export const register = (data) => request.post('/auth/register', data)

/**
 * 退出登录
 *
 * 请求：POST /api/auth/logout
 * 请求头：Authorization: Bearer <token>（由请求拦截器自动注入）
 * 后端行为：
 *  1. 将当前 Access Token 加入 Redis 黑名单（key: token:blacklist:{token}）
 *  2. 删除 Redis 中的 token 缓存（key: token:access:{userId}）
 * 响应：R<Void>
 *
 * @returns {Promise}
 */
export const logout = () => request.post('/auth/logout')

/**
 * 获取当前登录用户信息
 *
 * 请求：GET /api/users/me
 * 请求头：Authorization: Bearer <token>（自动注入）
 * 响应：R<UserInfo>
 *   - id, username, nickname, email, phone, avatar, status
 *   - roles:       角色标识列表，如 ['ADMIN', 'USER']
 *   - permissions: 权限标识列表，如 ['system:user:query', 'product:list']
 *
 * 使用场景：
 *  1. 路由守卫（router/index.js）：token 存在但 store 无用户信息时调用，懒加载用户数据
 *  2. 保存个人信息后：刷新 store 中的用户信息以更新页面显示
 *
 * @returns {Promise}
 */
export const getCurrentUser = () => request.get('/users/me')

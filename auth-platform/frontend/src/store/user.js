/**
 * 用户状态管理仓库（Pinia Store）
 *
 * 职责：
 *  1. 持久化存储 JWT Token（存入 localStorage，页面刷新后恢复）
 *  2. 缓存当前登录用户信息（userInfo），避免每次页面跳转都重新请求接口
 *  3. 提供 login / logout / fetchUserInfo / hasPermission 等操作方法
 *  4. 供路由守卫、请求拦截器、各页面组件使用
 *
 * 数据流：
 *  用户登录 → login() → 保存 token + userInfo → 后续请求自动携带 token
 *  页面刷新 → 从 localStorage 恢复 token → 路由守卫调用 fetchUserInfo() 重建 userInfo
 *  token 过期 → 请求拦截器检测到 401 → 调用 logout() 清除本地数据 → 跳转登录页
 */

import {defineStore} from 'pinia'
import {ref} from 'vue'
import {login as loginApi, getCurrentUser} from '../api/auth'

/**
 * 使用组合式 API 风格定义 store
 * defineStore(storeId, setup)：
 *  - storeId：唯一标识符 'user'，用于 Pinia devtools 中区分不同 store
 *  - setup：类似 Vue 组合式 API 的 setup 函数，返回需要暴露的状态和方法
 */
export const useUserStore = defineStore('user', () => {

    // ——— 响应式状态 ———

    /**
     * JWT Access Token
     * 初始值从 localStorage 中恢复（key: 'auth_token'），实现页面刷新后保持登录状态。
     * token 为空字符串时表示未登录。
     */
    const token = ref(localStorage.getItem('auth_token') || '')

    /**
     * 当前登录用户信息对象
     * 结构：{
     *   id:          用户 ID
     *   username:    用户名
     *   nickname:    昵称
     *   email:       邮箱
     *   phone:       手机号
     *   avatar:      头像 URL
     *   status:      账号状态（1=正常，0=禁用）
     *   roles:       角色标识列表，如 ['SUPER_ADMIN', 'USER']
     *   permissions: 权限标识列表，如 ['system:user:query', 'product:list']
     * }
     * 页面刷新后为 null（不持久化），由路由守卫在需要时调用 fetchUserInfo() 重新获取
     */
    const userInfo = ref(null)

    // ——— 操作方法 ———

    /**
     * 用户登录
     *
     * 调用登录接口，将返回的 Access Token 和 Refresh Token 存入 localStorage，
     * 并将用户信息缓存到 store 中。
     *
     * Token 存储策略：
     *  - Access Token（auth_token）：有效期较短（默认 2h），用于 API 请求认证
     *  - Refresh Token（auth_refresh_token）：有效期较长（默认 7 天），用于无感刷新 token
     *    （注意：当前版本前端未实现自动刷新逻辑，可后续扩展）
     *
     * @param {Object} form - 登录表单 { username, password }
     * @returns {Promise} 登录接口的响应（R<LoginResponse>）
     */
    async function login(form) {
        const res = await loginApi(form)

        // 将 Access Token 存入 Pinia store（用于请求拦截器实时读取）
        token.value = res.data.accessToken

        // 将完整用户信息存入 store（避免登录后还要单独请求 /users/me）
        userInfo.value = res.data.userInfo

        // 持久化 Access Token 到 localStorage（页面刷新后可恢复登录状态）
        localStorage.setItem('auth_token', token.value)

        // 持久化 Refresh Token（为后续实现无感续期做准备）
        localStorage.setItem('auth_refresh_token', res.data.refreshToken)

        return res
    }

    /**
     * 获取当前登录用户信息（懒加载）
     *
     * 使用场景：
     *  1. 页面刷新后，token 从 localStorage 恢复，但 userInfo 为 null
     *     此时路由守卫会调用此方法重新获取用户信息
     *  2. 保存个人信息后，调用此方法刷新 store 中的缓存数据
     *
     * @returns {Promise<Object>} 用户信息对象
     */
    async function fetchUserInfo() {
        const res = await getCurrentUser()
        // 将最新的用户信息更新到 store
        userInfo.value = res.data
        return res.data
    }

    /**
     * 退出登录（客户端清理）
     *
     * 清除 Pinia store 中的 token 和 userInfo，
     * 同时删除 localStorage 中的持久化数据。
     *
     * 注意：此方法仅清理客户端状态，实际向后端发送注销请求的调用在 Layout.vue 的
     * handleLogout() 中，后端会将 token 加入黑名单。
     *
     * 即使后端注销请求失败（网络问题），此方法仍会清除本地数据，保证客户端安全退出。
     */
    function logout() {
        // 清除内存中的状态
        token.value = ''
        userInfo.value = null
        // 清除 localStorage 中的持久化数据
        localStorage.removeItem('auth_token')
        localStorage.removeItem('auth_refresh_token')
    }

    /**
     * 检查当前用户是否拥有指定权限
     *
     * 权限检查逻辑：
     *  1. 未登录（无 userInfo）→ 返回 false
     *  2. 拥有 SUPER_ADMIN 角色 → 直接返回 true（超级管理员拥有所有权限）
     *  3. 检查 userInfo.permissions 列表中是否包含指定的权限标识
     *
     * 使用示例：
     *  const userStore = useUserStore()
     *  if (userStore.hasPermission('system:user:add')) {
     *    // 显示新增用户按钮
     *  }
     *
     * @param {string} perm - 权限标识（如 'system:user:query'）
     * @returns {boolean} 是否有权限
     */
    function hasPermission(perm) {
        if (!userInfo.value) return false
        // 超级管理员拥有所有权限，跳过具体权限检查
        if (userInfo.value.roles?.includes('SUPER_ADMIN')) return true
        // 检查 permissions 数组中是否包含该权限标识
        return userInfo.value.permissions?.includes(perm) ?? false
    }

    // 暴露 store 的状态和方法供组件/路由守卫使用
    return {token, userInfo, login, fetchUserInfo, logout, hasPermission}
})

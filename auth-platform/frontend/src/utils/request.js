/**
 * HTTP 请求工具模块
 *
 * 基于 Axios 封装，提供以下能力：
 *  1. 统一基础路径：所有请求以 /api 为前缀（通过 Vite 代理转发到后端 8080 端口）
 *  2. 请求拦截器：自动将 Pinia 中存储的 JWT 令牌注入到 Authorization 请求头
 *  3. 响应拦截器：
 *     - 统一处理业务层错误（后端返回的 code !== 200）
 *     - 自动处理 401 未认证：清除 token 并跳转到登录页
 *     - 弹出 ElMessage 错误提示，避免每个接口调用处重复处理
 *
 * 使用方式：
 *   import request from '@/utils/request'
 *   const res = await request.get('/users', { params: { pageNum: 1 } })
 *   // res 直接是后端的 R<T> 结构体（响应拦截器返回 response.data）
 */

import axios from 'axios'
import {ElMessage} from 'element-plus'
import {useUserStore} from '../store/user'
import router from '../router'

/**
 * 创建 Axios 实例
 * - baseURL: 所有接口的公共前缀，与 vite.config.js 中的代理配置对应
 *   开发环境下 /api → http://localhost:8080
 * - timeout: 请求超时时间（毫秒），超过此时间自动报错
 */
const request = axios.create({
    baseURL: '/api',
    timeout: 15000,
    withCredentials: true,
})

/**
 * 请求拦截器
 * 在每个请求发出前执行，将 JWT Access Token 注入到 Authorization 头部
 *
 * 为什么使用函数而非直接访问 store：
 *   Pinia store 必须在 Vue app 创建后才能访问，而拦截器在模块加载时定义。
 *   因此在请求时（而不是模块加载时）调用 useUserStore()，确保 Pinia 已初始化。
 *
 * @param {import('axios').InternalAxiosRequestConfig} config - 请求配置对象
 * @returns {import('axios').InternalAxiosRequestConfig} 修改后的配置对象
 */
request.interceptors.request.use((config) => {
    const userStore = useUserStore()
    // 如果 store 中有 token，则将其以 Bearer 格式写入请求头
    // 后端的 JwtAuthenticationFilter 会从该头部解析 token
    if (userStore.token) {
        config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
})

/**
 * 响应拦截器
 * 对所有接口的响应进行统一处理，分为两个回调：
 *  1. 成功回调（HTTP 2xx）：检查业务层状态码
 *  2. 失败回调（HTTP 4xx/5xx）：处理网络/HTTP 层错误
 */
request.interceptors.response.use(
    /**
     * 成功响应处理（HTTP 状态码为 2xx）
     * 后端统一返回 R<T> 结构：{ code, message, data }
     *
     * @param {import('axios').AxiosResponse} response - Axios 响应对象
     * @returns {Object} 当业务成功时返回 response.data（即 R<T> 对象本身）
     * @throws {Error} 当业务码非 200 时，弹出错误并抛出 Promise.reject
     */
    (response) => {
        const res = response.data // 取出后端返回的 JSON 主体（R<T> 结构）

        // 检查后端自定义业务状态码是否为 200（成功）
        if (res.code !== 200) {
            // 弹出错误提示（fallback：若后端未返回 message 则显示通用提示）
            ElMessage.error(res.message || '请求失败')

            // 如果业务码为 401，说明 token 已失效或过期
            // 需要清除本地 token 并跳转到登录页
            if (res.code === 401) {
                useUserStore().logout() // 清除 Pinia store 和 localStorage 中的 token
                router.push('/login')
            }

            // 将请求标记为失败，调用处可以通过 .catch() 或 try/catch 捕获
            return Promise.reject(new Error(res.message))
        }

        // 业务成功：直接返回 R<T> 对象
        // 调用方通过 res.data 获取实际业务数据
        return res
    },

    /**
     * 失败响应处理（HTTP 状态码非 2xx，如 404、500、网络超时等）
     *
     * @param {import('axios').AxiosError} error - Axios 错误对象
     * @returns {Promise<never>} 始终返回 rejected Promise
     */
    (error) => {
        // 优先取后端返回的 JSON message；若无（如网络断开）则取 axios 的 message
        ElMessage.error(error.response?.data?.message || error.message)

        // HTTP 401：通常是 Spring Security 过滤器拦截的未认证请求
        // （区别于业务层 401：这里是 HTTP 层的 401 响应）
        if (error.response?.status === 401) {
            useUserStore().logout()
            router.push('/login')
        }

        return Promise.reject(error)
    }
)

export default request

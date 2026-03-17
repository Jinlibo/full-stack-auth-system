/**
 * Vue Router 路由配置
 *
 * 本文件定义了整个前端应用的路由结构，并配置了全局导航守卫。
 *
 * 路由结构：
 *  /login           → 登录页（公开，不需要认证）
 *  /oauth-login     → OAuth2 登录授权页（公开，由 OAuth2 授权服务器跳转过来）
 *  /oauth-consent   → OAuth2 授权确认页（公开）
 *  /oauth-error     → OAuth2 错误页（公开）
 *  /                → 主布局（需要认证，使用 Layout.vue 作为外层框架）
 *    /dashboard         → 仪表盘首页
 *    /system/user       → 用户管理
 *    /system/role       → 角色管理
 *    /system/permission → 权限管理
 *    /product/list      → 产品管理
 *    /profile           → 个人中心
 *  /:pathMatch(.*)*  → 未匹配路由，重定向到首页
 *
 * 导航守卫逻辑：
 *  1. 更新页面标题（document.title）
 *  2. 公开页面（meta.public=true）直接放行
 *  3. 私有页面检查 token：无 token 则跳转登录页
 *  4. 有 token 但 store 中无 userInfo（页面刷新场景）：
 *     调用 fetchUserInfo() 从服务器重新获取用户信息
 *     若接口失败（token 已过期）：清除 token 并跳转登录页
 */

import {createRouter, createWebHistory} from 'vue-router'
import {useUserStore} from '../store/user'

/**
 * 路由表配置
 * 使用 () => import() 懒加载语法（动态导入），
 * 构建时 Vite 会将每个路由组件打包为独立的 chunk（代码分割），
 * 减小首屏加载体积，提升性能
 */
const routes = [
    // ——— 公开页面（无需登录即可访问）———

    {
        path: '/login',
        name: 'Login',
        // 懒加载登录页组件
        component: () => import('../views/auth/Login.vue'),
        // meta.public=true：导航守卫中标记为公开路由，直接放行
        meta: {title: '登录', public: true},
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/auth/Register.vue'),
        meta: {title: '注册', public: true},
    },
    {
        path: '/oauth-login',
        name: 'OAuthLogin',
        // OAuth2 登录授权页：当第三方应用发起 OAuth2 授权时，Spring Authorization Server
        // 会将用户重定向到此页面进行登录（类似于 GitHub 的 OAuth 登录页）
        component: () => import('../views/oauth/OAuthLogin.vue'),
        meta: {title: 'OAuth 登录授权', public: true},
    },
    {
        path: '/oauth-consent',
        name: 'OAuthConsent',
        // OAuth2 授权确认页：用户登录后需要在此页面确认是否授权第三方应用访问其数据
        component: () => import('../views/oauth/OAuthConsent.vue'),
        meta: {title: '授权确认', public: true},
    },
    {
        path: '/oauth-error',
        name: 'OAuthError',
        // OAuth2 流程发生错误时的展示页（如 redirect_uri 不匹配、客户端不存在等）
        component: () => import('../views/oauth/OAuthError.vue'),
        meta: {title: '授权失败', public: true},
    },

    // ——— 私有页面（需要登录认证，使用 Layout.vue 作为布局外壳）———

    {
        path: '/',
        // 主布局组件：包含侧边栏、顶部导航栏和 <router-view> 内容区
        component: () => import('../components/Layout.vue'),
        // 访问根路径时自动重定向到仪表盘
        redirect: '/dashboard',
        // 子路由：渲染在 Layout.vue 的 <router-view> 中
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/Dashboard.vue'),
                meta: {title: '仪表盘'},
            },
            {
                path: 'system/user',
                name: 'UserManage',
                component: () => import('../views/system/UserManage.vue'),
                meta: {title: '用户管理'},
            },
            {
                path: 'system/role',
                name: 'RoleManage',
                component: () => import('../views/system/RoleManage.vue'),
                meta: {title: '角色管理'},
            },
            {
                path: 'system/permission',
                name: 'PermissionManage',
                component: () => import('../views/system/PermissionManage.vue'),
                meta: {title: '权限管理'},
            },
            {
                path: 'product/list',
                name: 'ProductList',
                component: () => import('../views/product/ProductList.vue'),
                meta: {title: '产品管理'},
            },
            {
                path: 'profile',
                name: 'Profile',
                component: () => import('../views/auth/Profile.vue'),
                meta: {title: '个人中心'},
            },
        ],
    },

    // ——— 通配路由：所有未匹配的路径重定向到首页 ———
    {path: '/:pathMatch(.*)*', redirect: '/'},
]

/**
 * 创建路由实例
 * - history: createWebHistory() 使用 HTML5 History 模式（无 # 号的 URL）
 *   需要服务器配置 fallback 到 index.html，否则刷新页面会 404
 *   开发环境下 Vite dev server 已自动处理
 */
const router = createRouter({
    history: createWebHistory(),
    routes,
})

/**
 * 全局前置导航守卫
 * 在每次路由跳转前执行，用于实现访问控制和用户信息初始化
 *
 * @param {import('vue-router').RouteLocationNormalized} to   - 即将进入的目标路由
 * @param {import('vue-router').RouteLocationNormalized} from - 当前导航正要离开的路由
 * @param {Function} next - 调用该方法以 resolve 此钩子：
 *   next()          → 放行，允许跳转
 *   next('/login')  → 重定向到登录页
 */
router.beforeEach(async (to, from, next) => {
    // 第一步：更新页面标题
    // 格式："{路由标题} - 认证平台"，若无标题则直接显示"认证平台"
    document.title = to.meta.title ? `${to.meta.title} - 认证平台` : '认证平台'

    // 第二步：公开路由直接放行（登录页、OAuth 相关页面）
    if (to.meta.public) return next()

    // 第三步：获取 Pinia user store
    // 注意：必须在函数体内调用 useUserStore()，不能在顶层调用
    // 因为 Pinia 必须在 Vue app.use(pinia) 之后才可以使用
    const userStore = useUserStore()

    // 第四步：检查 token
    // token 为空字符串时表示未登录，重定向到登录页
    if (!userStore.token) return next('/login')

    // 第五步：处理页面刷新后 userInfo 为 null 的情况
    // 场景：用户刷新浏览器，localStorage 中的 token 被恢复到 store，
    //       但 userInfo 是内存数据，刷新后丢失，需要重新从服务器拉取
    if (!userStore.userInfo) {
        try {
            // 使用持久化的 token 请求用户信息接口
            await userStore.fetchUserInfo()
        } catch (e) {
            // 如果接口返回 401（token 已过期或被拉黑），清除本地数据并跳转登录页
            userStore.logout()
            return next('/login')
        }
    }

    // 第六步：通过所有检查，放行路由跳转
    next()
})

export default router

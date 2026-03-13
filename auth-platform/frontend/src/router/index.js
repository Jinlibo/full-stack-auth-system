import {createRouter, createWebHistory} from 'vue-router'
import {useUserStore} from '../store/user'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/auth/Login.vue'),
        meta: {title: '登录', public: true}
    },
    {
        path: '/oauth-login',
        name: 'OAuthLogin',
        component: () => import('../views/oauth/OAuthLogin.vue'),
        meta: {title: 'OAuth 登录授权', public: true}
    },
    {
        path: '/oauth-consent',
        name: 'OAuthConsent',
        component: () => import('../views/oauth/OAuthConsent.vue'),
        meta: {title: '授权确认', public: true}
    },
    {
        path: '/oauth-error',
        name: 'OAuthError',
        component: () => import('../views/oauth/OAuthError.vue'),
        meta: {title: '授权失败', public: true}
    },
    {
        path: '/',
        component: () => import('../components/Layout.vue'),
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/Dashboard.vue'),
                meta: {title: '仪表盘'}
            },
            {
                path: 'system/user',
                name: 'UserManage',
                component: () => import('../views/system/UserManage.vue'),
                meta: {title: '用户管理'}
            },
            {
                path: 'system/role',
                name: 'RoleManage',
                component: () => import('../views/system/RoleManage.vue'),
                meta: {title: '角色管理'}
            },
            {
                path: 'system/permission',
                name: 'PermissionManage',
                component: () => import('../views/system/PermissionManage.vue'),
                meta: {title: '权限管理'}
            },
            {
                path: 'product/list',
                name: 'ProductList',
                component: () => import('../views/product/ProductList.vue'),
                meta: {title: '产品管理'}
            },
            {
                path: 'profile',
                name: 'Profile',
                component: () => import('../views/auth/Profile.vue'),
                meta: {title: '个人中心'}
            },
        ]
    },
    {path: '/:pathMatch(.*)*', redirect: '/'}
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach(async (to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} - 认证平台` : '认证平台'
    if (to.meta.public) return next()

    const userStore = useUserStore()
    if (!userStore.token) return next('/login')

    if (!userStore.userInfo) {
        try {
            await userStore.fetchUserInfo()
        } catch (e) {
            userStore.logout();
            return next('/login')
        }
    }
    next()
})

export default router

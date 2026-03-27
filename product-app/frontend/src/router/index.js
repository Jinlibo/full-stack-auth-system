import {createRouter, createWebHistory} from 'vue-router'
import {useUserStore} from '../store/user'

const routes = [
    {
        path: '/auth',
        component: () => import('../components/ProductAuthLayout.vue'),
        children: [
            {
                path: '/login',
                name: 'Login',
                component: () => import('../views/auth/Login.vue'),
                meta: {title: '登录', public: true}
            },
            {
                path: '/register',
                name: 'Register',
                component: () => import('../views/auth/Register.vue'),
                meta: {title: '注册', public: true}
            },
            {
                path: '/oauth/callback',
                name: 'OAuthCallback',
                component: () => import('../views/auth/OAuthCallback.vue'),
                meta: {title: 'OAuth登录中...', public: true}
            },
        ]
    },
    {
        path: '/', component: () => import('../components/Layout.vue'), redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/Dashboard.vue'),
                meta: {title: '仪表盘'}
            },
            {
                path: 'profile',
                name: 'Profile',
                component: () => import('../views/auth/Profile.vue'),
                meta: {title: '个人中心'}
            },
            {
                path: 'users',
                name: 'UserList',
                component: () => import('../views/user/UserList.vue'),
                meta: {title: '用户管理'}
            },
            {
                path: 'system/roles',
                name: 'RoleManage',
                component: () => import('../views/system/RoleManage.vue'),
                meta: {title: '角色管理'}
            },
            {
                path: 'system/permissions',
                name: 'PermissionManage',
                component: () => import('../views/system/PermissionManage.vue'),
                meta: {title: '权限管理'}
            },
        ]
    },
    {path: '/:pathMatch(.*)*', redirect: '/'}
]

const router = createRouter({history: createWebHistory(), routes})

router.beforeEach(async (to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} - 产品应用` : '产品应用'
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

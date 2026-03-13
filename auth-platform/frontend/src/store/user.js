import {defineStore} from 'pinia'
import {ref} from 'vue'
import {login as loginApi, getCurrentUser} from '../api/auth'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('auth_token') || '')
    const userInfo = ref(null)

    async function login(form) {
        const res = await loginApi(form)
        token.value = res.data.accessToken
        userInfo.value = res.data.userInfo
        localStorage.setItem('auth_token', token.value)
        localStorage.setItem('auth_refresh_token', res.data.refreshToken)
        return res
    }

    async function fetchUserInfo() {
        const res = await getCurrentUser()
        userInfo.value = res.data
        return res.data
    }

    function logout() {
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('auth_token')
        localStorage.removeItem('auth_refresh_token')
    }

    function hasPermission(perm) {
        if (!userInfo.value) return false
        if (userInfo.value.roles?.includes('SUPER_ADMIN')) return true
        return userInfo.value.permissions?.includes(perm)
    }

    return {token, userInfo, login, fetchUserInfo, logout, hasPermission}
})

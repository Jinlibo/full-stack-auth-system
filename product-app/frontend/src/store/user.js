import {defineStore} from 'pinia'
import {ref} from 'vue'
import {login as loginApi, getCurrentUser} from '../api/auth'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('product_token') || '')
    const userInfo = ref(null)

    async function login(form) {
        const res = await loginApi(form)
        setAuth(res.data)
        return res
    }

    function setAuth(data) {
        token.value = data.accessToken
        userInfo.value = data.userInfo
        localStorage.setItem('product_token', data.accessToken)
    }

    async function fetchUserInfo() {
        const res = await getCurrentUser();
        userInfo.value = res.data;
        return res.data
    }

    function logout() {
        token.value = '';
        userInfo.value = null;
        localStorage.removeItem('product_token');
    }

    return {token, userInfo, login, setAuth, fetchUserInfo, logout}
})

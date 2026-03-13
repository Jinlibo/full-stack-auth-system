import axios from 'axios'
import {ElMessage} from 'element-plus'
import {useUserStore} from '../store/user'
import router from '../router'

const request = axios.create({baseURL: '/api', timeout: 15000})

request.interceptors.request.use(config => {
    const userStore = useUserStore()
    if (userStore.token) config.headers.Authorization = `Bearer ${userStore.token}`
    return config
})

request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.message || '请求失败')
            if (res.code === 401) {
                useUserStore().logout();
                router.push('/login')
            }
            return Promise.reject(new Error(res.message))
        }
        return res
    },
    error => {
        ElMessage.error(error.response?.data?.message || error.message)
        if (error.response?.status === 401) {
            useUserStore().logout();
            router.push('/login')
        }
        return Promise.reject(error)
    }
)
export default request

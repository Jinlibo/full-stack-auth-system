import axios from 'axios'
import {ElMessage} from 'element-plus'
import {useUserStore} from '../store/user'
import router from '../router'

const request = axios.create({baseURL: '/api', timeout: 15000})
request.interceptors.request.use(config => {
    const t = useUserStore().token
    if (t) config.headers.Authorization = `Bearer ${t}`
    return config
})
request.interceptors.response.use(
    res => {
        if (res.data.code !== 200) {
            ElMessage.error(res.data.message);
            if (res.data.code === 401) {
                useUserStore().logout();
                router.push('/login')
            }
            return Promise.reject(new Error(res.data.message))
        }
        return res.data
    },
    err => {
        ElMessage.error(err.response?.data?.message || err.message);
        if (err.response?.status === 401) {
            useUserStore().logout();
            router.push('/login')
        }
        return Promise.reject(err)
    }
)
export default request

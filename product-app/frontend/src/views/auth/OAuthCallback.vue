<template>
  <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center;">
    <el-card style="width: 400px; text-align: center;">
      <div v-if="!error">
        <el-icon :size="48" style="color: #409EFF; margin-bottom: 16px;">
          <Loading/>
        </el-icon>
        <p style="font-size: 16px;">正在完成登录授权...</p>
      </div>
      <div v-else>
        <el-icon :size="48" style="color: #F56C6C; margin-bottom: 16px;">
          <CircleCloseFilled/>
        </el-icon>
        <p style="font-size: 16px; color: #F56C6C;">{{ error }}</p>
        <el-button style="margin-top: 16px;" type="primary" @click="$router.push('/login')">返回登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {useUserStore} from '../../store/user'
import {oauthCallback, bindOAuth} from '../../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const error = ref('')

onMounted(async () => {
  const code = route.query.code
  const state = route.query.state
  if (!code) {
    error.value = '授权码缺失';
    return
  }

  // 校验state防CSRF
  const savedState = sessionStorage.getItem('oauth_state')
  if (savedState && state !== savedState) {
    error.value = 'State不匹配，可能存在CSRF攻击';
    return
  }
  sessionStorage.removeItem('oauth_state')

  const action = sessionStorage.getItem('oauth_action')
  sessionStorage.removeItem('oauth_action')

  try {
    if (action === 'bind') {
      // 绑定模式：将OAuth账号绑定到当前已登录用户
      const res = await bindOAuth({code, state})
      userStore.userInfo = res.data
      ElMessage.success('绑定成功')
      router.push('/profile')
    } else {
      // 登录模式：用OAuth账号登录（新建或匹配本地用户）
      const res = await oauthCallback({code, state})
      userStore.setAuth(res.data)
      ElMessage.success('OAuth登录成功')
      router.push('/')
    }
  } catch (e) {
    error.value = (action === 'bind' ? '账号绑定失败: ' : '授权登录失败: ') + (e.message || '未知错误')
  }
})
</script>

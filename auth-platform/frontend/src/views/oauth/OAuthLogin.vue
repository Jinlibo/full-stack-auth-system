<template>
    <div class="login-core">
      <div class="welcome-header">
        <h2>请求授权连入</h2>
        <p>登录以确认同意下发身份凭证</p>
      </div>

      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        class="premium-form" 
        @submit.prevent="handleLogin"
        size="large"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账户 / Username" prefix-icon="User" />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            placeholder="密码 / Password" 
            type="password" 
            prefix-icon="Lock"
            show-password 
          />
        </el-form-item>
        
        <el-form-item>
          <el-button 
            class="glow-button" 
            native-type="submit" 
            :loading="loading" 
          >
            <span>{{ loading ? '防线查验中...' : '核验并继续授权' }}</span>
            <el-icon class="btn-icon" v-if="!loading"><ArrowRight/></el-icon>
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 错误提示 -->
      <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          :closable="false"
          show-icon
          style="margin-top: -12px; margin-bottom: 24px;"
      />

      <div class="form-footer">
        <span class="hint">安全提醒: 这是系统内建底层验证网关</span>
      </div>
    </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import axios from 'axios'


const AUTH_BASE = 'http://localhost:8080'

const formRef = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({username: '', password: ''})

const rules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  errorMsg.value = ''

  try {
    const params = new URLSearchParams()
    params.append('username', form.username)
    params.append('password', form.password)

    const res = await axios.post(`${AUTH_BASE}/login`, params, {
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      withCredentials: true,
    })

    const nickname = res.data?.nickname || res.data?.username || form.username
    sessionStorage.setItem('oauth_login_nickname', nickname)

    const redirectUrl = res.data?.redirectUrl || AUTH_BASE
    window.location.href = redirectUrl

  } catch (e) {
    const rawMsg = e.response?.data?.message || ''
    errorMsg.value = rawMsg.toLowerCase().includes('bad credentials') || rawMsg.includes('不存在') || rawMsg.includes('禁用')
        ? '用户名或密码错误，请重新输入'
        : (rawMsg || '登录失败，请检查网络或稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-core {
  width: 100%;
  max-width: 340px;
}

.welcome-header { margin-bottom: 40px; }
.welcome-header h2 { font-size: 28px; font-weight: 700; color: #fff; margin: 0 0 8px; }
.welcome-header p { font-size: 14px; color: #71717a; margin: 0; }

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 32px;
  font-size: 13px;
}

.hint { color: #52525b; }
</style>

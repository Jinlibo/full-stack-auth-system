<template>
  <div class="oauth-login-bg">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="card-header">
          <div class="platform-title">认证授权平台</div>
          <div class="platform-sub">请登录以继续授权</div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              autocomplete="username"
              placeholder="用户名"
              prefix-icon="User"
              size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              autocomplete="current-password"
              placeholder="密码"
              prefix-icon="Lock"
              show-password
              size="large"
              type="password"
          />
        </el-form-item>
        <el-form-item>
          <el-button
              :loading="loading"
              size="large"
              style="width: 100%"
              type="primary"
              @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
          v-if="errorMsg"
          :closable="false"
          :title="errorMsg"
          style="margin-top: 8px;"
          type="error"
      />
    </el-card>
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
    // POST as form-urlencoded so Spring Security's UsernamePasswordAuthenticationFilter picks it up.
    // withCredentials: true ensures the session cookie is stored in the browser.
    const params = new URLSearchParams()
    params.append('username', form.username)
    params.append('password', form.password)

    const res = await axios.post(`${AUTH_BASE}/login`, params, {
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      withCredentials: true,
    })

    // Success handler returns { redirectUrl: "http://localhost:8080/oauth2/authorize?..." }
    // Navigate there directly so the browser carries the session cookie.
    const redirectUrl = res.data?.redirectUrl || AUTH_BASE
    window.location.href = redirectUrl
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '登录失败，请检查用户名和密码'
    errorMsg.value = msg
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.oauth-login-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
}

.card-header {
  text-align: center;
}

.platform-title {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
}

.platform-sub {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>

<template>
  <div class="login-bg">
    <!-- Decorative blobs -->
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>

    <div class="login-wrapper">
      <!-- Left brand panel -->
      <div class="brand-panel">
        <div class="brand-logo">
          <svg fill="none" height="48" viewBox="0 0 48 48" width="48">
            <circle cx="24" cy="24" fill="rgba(255,255,255,0.15)" r="24"/>
            <path d="M24 10 L36 17 L36 31 L24 38 L12 31 L12 17 Z" fill="none" stroke="white" stroke-width="2"/>
            <circle cx="24" cy="24" fill="white" r="5"/>
          </svg>
        </div>
        <h1 class="brand-name">认证授权平台</h1>
        <p class="brand-desc">统一身份认证 · 权限管理 · OAuth2 授权</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-dot"></span>
            多应用统一认证入口
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            细粒度 RBAC 权限控制
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            标准 OAuth2 / OIDC 协议
          </div>
        </div>
      </div>

      <!-- Right login form -->
      <div class="form-panel">
        <div class="form-inner">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-sub">请使用管理员账号登录</p>

          <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @submit.prevent="handleLogin">
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
                  class="login-btn"
                  native-type="submit"
                  size="large"
                  type="primary"
                  @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-hint">
            <el-icon>
              <InfoFilled/>
            </el-icon>
            默认账号：admin &nbsp;/&nbsp; admin123
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {useUserStore} from '../../store/user'
import {ElMessage} from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({username: '', password: ''})
const rules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) { /* handled by interceptor */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1c2e 0%, #2d2f4a 50%, #1e3a5f 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.25;
  pointer-events: none;
}

.blob-1 {
  width: 500px;
  height: 500px;
  background: #667eea;
  top: -150px;
  left: -100px;
  animation: drift 12s ease-in-out infinite alternate;
}

.blob-2 {
  width: 400px;
  height: 400px;
  background: #764ba2;
  bottom: -100px;
  right: -80px;
  animation: drift 15s ease-in-out infinite alternate-reverse;
}

@keyframes drift {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(30px, 20px) scale(1.08);
  }
}

.login-wrapper {
  display: flex;
  width: 900px;
  max-width: calc(100vw - 48px);
  min-height: 520px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.4);
  position: relative;
  z-index: 1;
}

/* ——— Brand panel ——— */
.brand-panel {
  flex: 1;
  padding: 56px 48px;
  background: linear-gradient(150deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.2) 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-logo {
  margin-bottom: 24px;
}

.brand-name {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 10px;
  letter-spacing: 0.5px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0 0 40px;
  line-height: 1.6;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 14px;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #667eea;
  flex-shrink: 0;
}

/* ——— Form panel ——— */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 48px;
}

.form-inner {
  width: 100%;
  max-width: 320px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
}

.form-sub {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.45);
  margin: 0 0 32px;
}

.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none !important;
}

.login-form :deep(.el-input__wrapper:hover),
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  background: rgba(255, 255, 255, 0.12);
}

.login-form :deep(.el-input__inner) {
  color: #fff;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35);
}

.login-form :deep(.el-input__prefix-inner .el-icon),
.login-form :deep(.el-input__suffix-inner .el-icon) {
  color: rgba(255, 255, 255, 0.45);
}

.login-form :deep(.el-form-item__error) {
  color: #ff9f7f;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  transition: opacity 0.2s;
}

.login-btn:hover {
  opacity: 0.88;
}

.login-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
  margin-top: 16px;
}

@media (max-width: 700px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 40px 28px;
  }
}
</style>

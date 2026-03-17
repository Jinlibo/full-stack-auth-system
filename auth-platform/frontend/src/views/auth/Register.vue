<template>
  <div class="register-bg">
    <!-- 装饰性 blob -->
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>

    <div class="register-wrapper">
      <!-- 左侧品牌展示区（与 Login.vue 保持一致） -->
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

      <!-- 右侧注册表单 -->
      <div class="form-panel">
        <div class="form-inner">
          <h2 class="form-title">创建账号</h2>
          <p class="form-sub">填写信息完成注册</p>

          <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              class="register-form"
              @submit.prevent="handleRegister"
          >
            <!-- 用户名 -->
            <el-form-item prop="username">
              <el-input
                  v-model="form.username"
                  autocomplete="username"
                  placeholder="用户名（必填）"
                  prefix-icon="User"
                  size="large"
              />
            </el-form-item>

            <!-- 密码 -->
            <el-form-item prop="password">
              <el-input
                  v-model="form.password"
                  autocomplete="new-password"
                  placeholder="密码（至少 6 位）"
                  prefix-icon="Lock"
                  show-password
                  size="large"
                  type="password"
              />
            </el-form-item>

            <!-- 确认密码 -->
            <el-form-item prop="confirmPassword">
              <el-input
                  v-model="form.confirmPassword"
                  autocomplete="new-password"
                  placeholder="确认密码"
                  prefix-icon="Lock"
                  show-password
                  size="large"
                  type="password"
              />
            </el-form-item>

            <!-- 邮箱 -->
            <el-form-item prop="email">
              <el-input
                  v-model="form.email"
                  autocomplete="email"
                  placeholder="邮箱（必填）"
                  prefix-icon="Message"
                  size="large"
              />
            </el-form-item>

            <!-- 手机号（可选） -->
            <el-form-item prop="phone">
              <el-input
                  v-model="form.phone"
                  autocomplete="tel"
                  placeholder="手机号（可选）"
                  prefix-icon="Phone"
                  size="large"
              />
            </el-form-item>

            <!-- 提交按钮 -->
            <el-form-item>
              <el-button
                  :loading="loading"
                  class="register-btn"
                  native-type="submit"
                  size="large"
                  type="primary"
                  @click="handleRegister"
              >
                {{ loading ? '注册中...' : '立即注册' }}
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 已有账号跳转登录 -->
          <div class="login-hint">
            已有账号？
            <el-link type="primary" @click="router.push('/login')">立即登录</el-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {register} from '../../api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

// 表单数据
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
})

// 确认密码自定义校验
const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 手机号格式校验（可选字段）
const validatePhone = (rule, value, callback) => {
  if (!value) {
    // 手机号为可选，为空时直接通过
    callback()
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号格式'))
  } else {
    callback()
  }
}

// 表单校验规则
const rules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 2, max: 20, message: '用户名长度为 2~20 位', trigger: 'blur'},
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少 6 位', trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, validator: validateConfirmPassword, trigger: 'blur'},
  ],
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur'},
  ],
  phone: [
    {validator: validatePhone, trigger: 'blur'},
  ],
}

// 提交注册
const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    // 构造请求体，手机号为空时不传
    const payload = {
      username: form.username,
      password: form.password,
      email: form.email,
    }
    if (form.phone) payload.phone = form.phone

    await register(payload)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // 错误由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-bg {
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

.register-wrapper {
  display: flex;
  width: 900px;
  max-width: calc(100vw - 48px);
  min-height: 580px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.4);
  position: relative;
  z-index: 1;
}

/* ——— 品牌面板 ——— */
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

/* ——— 表单面板 ——— */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 48px;
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
  margin: 0 0 28px;
}

.register-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none !important;
}

.register-form :deep(.el-input__wrapper:hover),
.register-form :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  background: rgba(255, 255, 255, 0.12);
}

.register-form :deep(.el-input__inner) {
  color: #fff;
}

.register-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35);
}

.register-form :deep(.el-input__prefix-inner .el-icon),
.register-form :deep(.el-input__suffix-inner .el-icon) {
  color: rgba(255, 255, 255, 0.45);
}

.register-form :deep(.el-form-item__error) {
  color: #ff9f7f;
}

/* 减小表单项间距，防止字段过多时超出容器 */
.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  transition: opacity 0.2s;
}

.register-btn:hover {
  opacity: 0.88;
}

/* 已有账号提示 */
.login-hint {
  text-align: center;
  font-size: 13px;
  color: #c0c4cc;
  margin-top: 12px;
}

.login-hint :deep(.el-link) {
  font-size: 13px;
  vertical-align: baseline;
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

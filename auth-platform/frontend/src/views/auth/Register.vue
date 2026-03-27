<template>
  <div class="login-core">
    <div class="welcome-header">
      <h2>创建系统通行证</h2>
      <p>填写以下信息注册为您分配的网络身份</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      class="premium-form register-form"
      @submit.prevent="handleRegister"
      size="large"
    >
      <el-form-item prop="username" class="compact-item">
        <el-input v-model="form.username" placeholder="用户名 / Username" prefix-icon="User" />
      </el-form-item>

      <el-form-item prop="password" class="compact-item">
        <el-input
          v-model="form.password"
          placeholder="密码 / Password"
          type="password"
          prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <el-form-item prop="confirmPassword" class="compact-item">
        <el-input
          v-model="form.confirmPassword"
          placeholder="确认密码 / Confirm Password"
          type="password"
          prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <el-form-item prop="email" class="compact-item">
        <el-input v-model="form.email" placeholder="邮箱 / Email Address" prefix-icon="Message" />
      </el-form-item>

      <el-form-item prop="phone" class="compact-item">
        <el-input v-model="form.phone" placeholder="手机号 (可选) / Phone (Optional)" prefix-icon="Phone" />
      </el-form-item>

      <el-form-item class="action-item">
        <el-button
          class="glow-button register-glow"
          native-type="submit"
          :loading="loading"
        >
          <span>{{ loading ? '连接中...' : '注册通行许可' }}</span>
          <el-icon class="btn-icon" v-if="!loading"><Check/></el-icon>
        </el-button>
      </el-form-item>
    </el-form>

    <div class="form-footer">
      <span class="hint">已有通行证？</span>
      <el-link class="register-link" :underline="false" @click="$router.push('/login')">立刻返回登录</el-link>
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

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback()
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号格式'))
  } else {
    callback()
  }
}

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

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const payload = {
      username: form.username,
      password: form.password,
      email: form.email,
    }
    if (form.phone) payload.phone = form.phone

    await register(payload)
    ElMessage.success('注册成功，请重新验证通行证登录')
    router.push('/login')
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-core { width: 100%; max-width: 340px; }
.welcome-header { margin-bottom: 30px; }
.welcome-header h2 { font-size: 28px; font-weight: 700; color: #fff; margin: 0 0 6px; }
.welcome-header p { font-size: 14px; color: #71717a; margin: 0; }

.compact-item { margin-bottom: 18px !important; }
.action-item { margin-top: 24px; margin-bottom: 0 !important; }

/* 专门针对注册页的粉色系强调覆写 */
html body .register-form :deep(.el-input__wrapper.is-focus) {
  border-color: #ec4899 !important;
  box-shadow: 0 0 0 2px rgba(236, 72, 153, 0.2) !important;
}

.register-glow {
  background: linear-gradient(to right, #ec4899, #8b5cf6) !important;
}
.register-glow::before {
  background: linear-gradient(to right, #f472b6, #a78bfa) !important;
}
.register-glow:hover {
  box-shadow: 0 10px 25px rgba(236, 72, 153, 0.4) !important;
}

.form-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 24px; font-size: 13px; }
.hint { color: #52525b; }
.register-link { color: #f472b6 !important; font-weight: 500; transition: all 0.2s;}
.register-link:hover { color: #fbcfe8 !important; }
</style>

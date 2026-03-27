<template>
    <div class="login-core">
      <div class="welcome-header">
        <h2>创建账号</h2>
        <p>填写以下信息完成注册</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="premium-form"
        @submit.prevent="handleRegister"
        size="large"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" clearable placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            placeholder="请输入密码（至少6位）" 
            prefix-icon="Lock" 
            show-password 
            type="password" 
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            placeholder="请再次输入密码" 
            prefix-icon="Lock" 
            show-password 
            type="password" 
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input v-model="form.email" clearable placeholder="请输入邮箱地址" prefix-icon="Message" />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" clearable placeholder="请输入昵称(可选)" prefix-icon="EditPen" />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input v-model="form.phone" clearable placeholder="请输入手机号(可选)" prefix-icon="Phone" />
        </el-form-item>

        <el-form-item class="action-item">
          <el-button 
            class="action-btn register-btn" 
            native-type="submit" 
            :loading="loading" 
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '立刻创建您的平台专属身份' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <span class="hint">已有账号？</span>
        <el-link class="register-link" :underline="false" @click="$router.push('/login')">立即登录</el-link>
      </div>
    </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {register} from '../../api/auth'
import {ElMessage} from 'element-plus'


const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: '',
  phone: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 2, max: 20, message: '用户名长度为 2~20 个字符', trigger: 'blur'},
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少6位', trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, validator: validateConfirmPassword, trigger: 'blur'},
  ],
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '请输入正确的邮箱格式', trigger: ['blur', 'change']},
  ],
}

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const {confirmPassword, ...payload} = form
    if (!payload.nickname) delete payload.nickname
    if (!payload.phone) delete payload.phone
    await register(payload)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-core { width: 100%; max-width: 360px; margin: 0 auto;}

.welcome-header { margin-bottom: 30px; }
.welcome-header h2 { font-size: 28px; font-weight: 800; color: #0f172a; margin: 0 0 6px; }
.welcome-header p { font-size: 14px; color: #64748b; margin: 0; }

.premium-form :deep(.el-form-item) { margin-bottom: 20px; }

.action-item { margin-top: 24px; }
.register-btn { background: linear-gradient(135deg, #11998e, #38ef7d); box-shadow: 0 8px 20px rgba(17, 153, 142, 0.25); }
.register-btn:hover { box-shadow: 0 12px 25px rgba(17, 153, 142, 0.35); }

.form-footer { display: flex; justify-content: center; gap: 8px; margin-top: 24px; font-size: 14px; }
.hint { color: #64748b; }
.register-link { color: #11998e !important; font-weight: 600; transition: color 0.2s; }
.register-link:hover { color: #0f7b6c !important; }
</style>

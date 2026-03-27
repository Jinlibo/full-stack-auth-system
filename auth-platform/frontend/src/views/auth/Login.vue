<template>
  <div class="login-core">
    <div class="welcome-header">
      <h2>欢迎探索系统</h2>
      <p>登录以访问您的管理员仪表盘</p>
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
          <span>{{ loading ? '验证中...' : '进入系统' }}</span>
          <el-icon class="btn-icon" v-if="!loading"><ArrowRight/></el-icon>
        </el-button>
      </el-form-item>
    </el-form>

    <div class="form-footer">
      <span class="hint">默认登录: admin / admin123</span>
      <el-link class="register-link" :underline="false" @click="$router.push('/register')">创建新账号</el-link>
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

.register-link {
  color: #d8b4fe !important;
  font-weight: 600;
  transition: all 0.2s;
}

.register-link:hover { color: #f3e8ff !important; }
</style>

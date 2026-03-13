<template>
  <div
      style="min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
    <el-card shadow="always" style="width: 420px">
      <template #header>
        <div style="text-align: center; font-size: 24px; font-weight: bold;">认证授权平台</div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large"/>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" placeholder="密码" prefix-icon="Lock" show-password size="large"
                    type="password"/>
        </el-form-item>
        <el-form-item>
          <el-button :loading="loading" size="large" style="width: 100%" type="primary" @click="handleLogin">登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div style="text-align: center; color: #999; font-size: 13px;">默认账号: admin / admin123</div>
    </el-card>
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

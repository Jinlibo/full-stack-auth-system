<template>
  <div
      style="min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);">
    <el-card shadow="always" style="width: 440px">
      <template #header>
        <div style="text-align: center; font-size: 24px; font-weight: bold;">产品应用</div>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="账号密码登录" name="password">
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
          <div style="text-align: center; color: #999; font-size: 13px;">默认: admin / admin123</div>
        </el-tab-pane>
        <el-tab-pane label="第三方登录" name="oauth">
          <div style="text-align: center; padding: 40px 0;">
            <p style="margin-bottom: 20px; color: #666;">使用认证平台账号登录</p>
            <el-button :loading="oauthLoading" size="large" style="width: 80%;" type="primary"
                       @click="handleOAuthLogin">
              <el-icon style="margin-right: 8px;">
                <Link/>
              </el-icon>
              认证平台授权登录
            </el-button>
            <p style="margin-top: 16px; font-size: 12px; color: #999;">将跳转到认证授权平台进行登录授权</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {useUserStore} from '../../store/user'
import {getOAuthUrl} from '../../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('password')
const formRef = ref(null)
const loading = ref(false)
const oauthLoading = ref(false)
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
    await userStore.login(form);
    ElMessage.success('登录成功');
    router.push('/')
  } catch (e) { /* handled */
  } finally {
    loading.value = false
  }
}

const handleOAuthLogin = async () => {
  oauthLoading.value = true
  try {
    const state = Math.random().toString(36).substring(7)
    sessionStorage.setItem('oauth_state', state)
    const res = await getOAuthUrl(state)
    window.location.href = res.data.authorizeUrl
  } catch (e) {
    ElMessage.error('获取授权地址失败')
  } finally {
    oauthLoading.value = false
  }
}
</script>

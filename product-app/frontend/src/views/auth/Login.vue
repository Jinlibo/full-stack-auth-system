<template>
  <div class="login-core">
    <div class="welcome-header">
      <h2>工作台登录验证</h2>
      <p>自由点选您偏好的系统准入方式</p>
    </div>

    <el-tabs v-model="activeTab" class="premium-tabs">
      <el-tab-pane label="底层直连凭证" name="password">
        <el-form ref="formRef" :model="form" :rules="rules" class="premium-form" @submit.prevent="handleLogin" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="输入您的组织内工号" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" placeholder="请输入隐蔽查验秘钥" type="password" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-button class="action-btn" native-type="submit" :loading="loading">
              {{ loading ? '防线查验中...' : '核验进入系统' }}
            </el-button>
          </el-form-item>
        </el-form>
        <div class="form-footer">
          <span class="hint">未持有凭证网络？</span>
          <el-link class="register-link" :underline="false" @click="$router.push('/register')">提交通行申请</el-link>
        </div>
      </el-tab-pane>

      <el-tab-pane label="上游协同中心认证" name="oauth">
        <div class="oauth-panel">
          <div class="oauth-shield">
            <div class="shield-inner"><el-icon :size="36"><Connection/></el-icon></div>
          </div>
          <h3 class="oauth-title">利用主站 Auth Platform 接管</h3>
          <p class="oauth-desc">您将通过安全隧道前往顶级鉴权中心，<br>完成零时差防线认证后系统将为您主动放行。</p>
          <el-button class="oauth-btn" :loading="oauthLoading" @click="handleOAuthLogin">
            <el-icon class="oauth-btn-icon" v-if="!oauthLoading"><Link/></el-icon>
            <span>立即启航单点授权登录</span>
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
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
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
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
    ElMessage.error('获取授权地址失败，请检查全栈鉴权平台服务是否就绪')
  } finally {
    oauthLoading.value = false
  }
}
</script>

<style scoped>
.login-core { width: 100%; max-width: 340px; }

.welcome-header { margin-bottom: 30px; }
.welcome-header h2 { font-size: 28px; font-weight: 800; color: #0f172a; margin: 0 0 6px; }
.welcome-header p { font-size: 14px; color: #64748b; margin: 0; }

.premium-tabs :deep(.el-tabs__nav-wrap::after) { background-color: #e2e8f0; height: 1px; }
.premium-tabs :deep(.el-tabs__active-bar) { background-color: #0ea5e9; height: 3px; border-radius: 3px; }
.premium-tabs :deep(.el-tabs__item) { font-size: 15px; font-weight: 600; color: #64748b; padding: 0 16px; transition: color 0.3s;}
.premium-tabs :deep(.el-tabs__item.is-active) { color: #0ea5e9; }
.premium-tabs :deep(.el-tabs__header) { margin-bottom: 30px; }

.form-footer { display: flex; justify-content: center; gap: 8px; margin-top: 24px; font-size: 14px; }
.hint { color: #64748b; }
.register-link { color: #0ea5e9 !important; font-weight: 600; transition: color 0.2s; }
.register-link:hover { color: #0284c7 !important; }

.oauth-panel { text-align: center; padding: 10px 0; display: flex; flex-direction: column; align-items: center; }

.oauth-shield {
  width: 80px; height: 80px;
  background: rgba(14, 165, 233, 0.1);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 24px;
}

.shield-inner {
  width: 60px; height: 60px;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  border-radius: 50%; color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 10px 25px rgba(14, 165, 233, 0.3);
}

.oauth-title { font-size: 18px; font-weight: 700; color: #0f172a; margin: 0 0 10px; }
.oauth-desc { font-size: 14px; color: #64748b; margin: 0 0 32px; line-height: 1.6; }

.oauth-btn {
  width: 100%; height: 50px;
  border-radius: 12px;
  background: #fff; border: 1px solid #e2e8f0;
  color: #0f172a; font-size: 15px; font-weight: 600;
  transition: all 0.3s; box-shadow: 0 4px 6px rgba(0,0,0,0.02);
  display: flex; align-items: center; justify-content: center; gap: 10px;
}

.oauth-btn:hover { background: #f8fafc; border-color: #cbd5e1; transform: translateY(-2px); box-shadow: 0 8px 15px rgba(0,0,0,0.05); color: #0ea5e9;}
.oauth-btn-icon { color: #0ea5e9; font-size: 18px; }
</style>

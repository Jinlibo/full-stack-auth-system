<template>
  <div class="consent-bg">
    <el-card class="consent-card" shadow="always">
      <!-- Loading state -->
      <div v-if="loading" class="center-content">
        <el-icon :size="40" class="spin" style="color: #409EFF;">
          <Loading/>
        </el-icon>
        <p style="margin-top: 12px; color: #606266;">正在加载授权信息...</p>
      </div>

      <!-- Error: client disabled or not found -->
      <div v-else-if="clientError" class="center-content">
        <el-icon :size="48" style="color: #F56C6C;">
          <CircleCloseFilled/>
        </el-icon>
        <p class="error-text">{{ clientError }}</p>
        <el-button style="margin-top: 16px;" @click="goBack">返回</el-button>
      </div>

      <!-- Normal consent UI -->
      <template v-else>
        <div class="app-header">
          <img v-if="clientInfo.logoUrl" :src="clientInfo.logoUrl" alt="logo" class="app-logo"/>
          <el-icon v-else :size="52" class="app-icon-default">
            <Monitor/>
          </el-icon>
          <div class="app-name">{{ clientInfo.name || clientId }}</div>
          <div v-if="clientInfo.description" class="app-desc">{{ clientInfo.description }}</div>
        </div>

        <el-divider/>

        <div class="scope-section">
          <div class="scope-title">该应用请求获取以下权限：</div>
          <el-space direction="vertical" fill style="width: 100%; margin-top: 12px;">
            <el-card
                v-for="scope in scopeList"
                :key="scope.value"
                class="scope-item"
                shadow="never"
            >
              <el-checkbox v-model="scope.approved" :label="scope.label" size="large">
                <div class="scope-label">{{ scope.label }}</div>
                <div class="scope-desc">{{ scope.desc }}</div>
              </el-checkbox>
            </el-card>
          </el-space>
        </div>

        <el-divider/>

        <!--
          Regular HTML form POST to the auth server's /oauth2/authorize endpoint.
          Using a real form (not fetch/axios) so the browser follows the 302 redirect
          automatically — this is how Spring Auth Server's consent handler works.
        -->
        <form
            ref="consentForm"
            :action="`${AUTH_BASE}/oauth2/authorize`"
            method="post"
            style="display: none;"
        >
          <input :value="clientId" name="client_id" type="hidden"/>
          <input :value="state" name="state" type="hidden"/>
          <input
              v-for="scope in approvedScopes"
              :key="scope"
              :value="scope"
              name="scope"
              type="hidden"
          />
        </form>

        <div class="action-row">
          <el-button :disabled="approvedScopes.length === 0" size="large" type="primary" @click="handleApprove">
            授权并继续
          </el-button>
          <el-button size="large" @click="goBack">拒绝</el-button>
        </div>

        <div class="footer-note">
          授权后，{{ clientInfo.name || clientId }} 将可以按上述权限访问您的账号信息。
          您可以随时在个人中心撤销授权。
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import axios from 'axios'

const AUTH_BASE = 'http://localhost:8080'

const route = useRoute()
const loading = ref(true)
const clientError = ref('')
const clientInfo = ref({})
const consentForm = ref(null)

// Query params passed by Spring Auth Server's consent redirect
const clientId = route.query.client_id || ''
const state = route.query.state || ''
const rawScope = route.query.scope || ''

// Scope metadata for display
const SCOPE_META = {
  openid: {label: 'OpenID', desc: '验证您的身份'},
  profile: {label: '基本资料', desc: '读取您的用户名和昵称'},
  email: {label: '邮箱地址', desc: '读取您的邮箱地址'},
  phone: {label: '手机号码', desc: '读取您的手机号码'},
}

const scopeList = ref([])

const approvedScopes = computed(() =>
    scopeList.value.filter(s => s.approved).map(s => s.value)
)

onMounted(async () => {
  if (!clientId) {
    clientError.value = '授权参数缺失，无法加载授权信息。'
    loading.value = false
    return
  }

  try {
    const res = await axios.get(`${AUTH_BASE}/api/oauth2/client-info`, {
      params: {clientId},
      withCredentials: true,
    })
    const info = res.data
    if (!info.found || !info.enabled) {
      clientError.value = `应用「${clientId}」不存在或已被停用，无法完成授权。`
      loading.value = false
      return
    }
    clientInfo.value = info

    // Build scope list with display names
    const scopes = rawScope ? rawScope.split(' ').filter(Boolean) : []
    scopeList.value = scopes.map(v => ({
      value: v,
      label: SCOPE_META[v]?.label || v,
      desc: SCOPE_META[v]?.desc || '',
      approved: true,
    }))
  } catch (e) {
    clientError.value = '加载授权信息失败，请刷新重试。'
  } finally {
    loading.value = false
  }
})

const handleApprove = () => {
  consentForm.value?.submit()
}

const goBack = () => {
  window.history.back()
}
</script>

<style scoped>
.consent-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 24px;
}

.consent-card {
  width: 480px;
  max-width: 100%;
}

.center-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 0;
}

.error-text {
  color: #F56C6C;
  font-size: 15px;
  margin-top: 16px;
  text-align: center;
}

.app-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0 8px;
}

.app-logo {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  object-fit: cover;
}

.app-icon-default {
  color: #909399;
}

.app-name {
  font-size: 20px;
  font-weight: bold;
  margin-top: 12px;
  color: #303133;
}

.app-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  text-align: center;
}

.scope-section {
  padding: 0 4px;
}

.scope-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.scope-item {
  border: 1px solid #e4e7ed !important;
}

.scope-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  display: inline;
}

.scope-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.action-row {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 8px 0;
}

.footer-note {
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  margin-top: 12px;
  line-height: 1.6;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

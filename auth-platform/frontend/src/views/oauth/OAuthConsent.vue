<template>
  <!-- ============================================================
       OAuth2 授权确认页（OAuthConsent）
       触发时机：用户已在 OAuthLogin 页登录后，Spring Auth Server 重定向到此页面。
       用途：展示请求授权的应用信息、当前登录用户、权限列表，
             用户点击"授权并继续"或"拒绝"后 POST 回 /oauth2/authorize。
       特别注意：授权确认必须通过原生 HTML form POST（而非 fetch/axios），
                  因为 Spring Auth Server 的 consent 端点会返回 302 重定向，
                  只有真实的表单提交才能让浏览器自动跟随带 session cookie 的重定向。
       ============================================================ -->
  <div class="consent-bg">
    <div class="consent-card">

      <!-- ——— 加载中状态 ——— -->
      <div v-if="loading" class="status-block">
        <div class="spin-ring">
          <el-icon :size="32" class="spin-icon">
            <Loading/>
          </el-icon>
        </div>
        <p class="status-text">正在加载授权信息...</p>
      </div>

      <!-- ——— 错误：应用不存在或已停用 ——— -->
      <div v-else-if="clientError" class="status-block">
        <div class="error-icon-wrap">
          <el-icon :size="36">
            <CircleCloseFilled/>
          </el-icon>
        </div>
        <p class="status-text error-text">{{ clientError }}</p>
        <el-button style="margin-top: 12px;" @click="goBack">返回</el-button>
      </div>

      <!-- ——— 正常授权确认 UI ——— -->
      <template v-else>

        <!-- 当前登录用户横幅 -->
        <div class="user-banner">
          <div class="user-avatar">
            {{ loginNickname.charAt(0).toUpperCase() }}
          </div>
          <div class="user-info">
            <div class="user-greeting">你好，<strong>{{ loginNickname }}</strong></div>
            <div class="user-desc">你正在使用此账号授权以下应用</div>
          </div>
        </div>

        <div class="divider"></div>

        <!-- 应用信息：图标 + 名称 + 描述 -->
        <div class="app-header">
          <img
              v-if="clientInfo.logoUrl"
              :src="clientInfo.logoUrl"
              alt="应用图标"
              class="app-logo"
          />
          <div v-else class="app-icon-default">
            <el-icon :size="28">
              <Monitor/>
            </el-icon>
          </div>
          <div class="app-name">{{ clientInfo.name || clientId }}</div>
          <div v-if="clientInfo.description" class="app-desc">{{ clientInfo.description }}</div>
        </div>

        <div class="divider"></div>

        <!-- 权限列表 -->
        <div class="scope-section">
          <div class="scope-title">该应用申请以下访问权限：</div>
          <div class="scope-list">
            <div
                v-for="scope in scopeList"
                :key="scope.value"
                class="scope-item"
                :class="{ 'scope-item--checked': scope.approved }"
                @click="scope.approved = !scope.approved"
            >
              <!-- 勾选框 -->
              <el-checkbox v-model="scope.approved" @click.stop/>
              <!-- 权限图标 -->
              <div class="scope-icon">
                <el-icon :size="16">
                  <component :is="scope.icon"/>
                </el-icon>
              </div>
              <!-- 权限文字 -->
              <div class="scope-text">
                <div class="scope-label">{{ scope.label }}</div>
                <div class="scope-desc">{{ scope.desc }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 隐藏的 HTML form：用于 POST 授权结果给 Spring Auth Server -->
        <!-- 必须使用原生 form POST，才能让浏览器携带 session cookie 并跟随 302 重定向 -->
        <form
            ref="consentForm"
            :action="`${AUTH_BASE}/oauth2/authorize`"
            method="post"
            style="display: none;"
        >
          <input :value="clientId" name="client_id" type="hidden"/>
          <input :value="state" name="state" type="hidden"/>
          <!-- 动态生成已勾选的 scope input（el-table-column 多个 name=scope 是合法的） -->
          <input
              v-for="scope in approvedScopes"
              :key="scope"
              type="hidden"
              :value="scope"
              name="scope"
          />
        </form>

        <!-- 操作按钮 -->
        <div class="action-row">
          <el-button
              :disabled="approvedScopes.length === 0"
              class="approve-btn"
              size="large"
              type="primary"
              @click="handleApprove"
          >授权并继续
          </el-button>
          <el-button size="large" @click="goBack">拒绝</el-button>
        </div>

        <!-- 底部说明 -->
        <p class="footer-note">
          授权后，{{ clientInfo.name || clientId }} 将可按上述权限读取你的账号信息。
          你可以随时在个人中心撤销授权。
        </p>

      </template>
    </div>
  </div>
</template>

<script setup>
/**
 * OAuth2 授权确认页 - 脚本逻辑
 *
 * 数据来源：
 *  - clientId / state / scope：来自 URL query 参数（Spring Auth Server 传递）
 *  - loginNickname：来自 sessionStorage（OAuthLogin.vue 登录成功后写入）
 *  - clientInfo（应用名称/描述/Logo）：调用 GET /api/oauth2/client-info 获取
 *
 * 授权流程：
 *  1. 用户勾选要授权的 scope → 点击"授权并继续"
 *  2. 触发 consentForm.submit()（原生 HTML form POST 到 /oauth2/authorize）
 *  3. Spring Auth Server 处理 consent → 生成 authorization_code → 302 重定向到 redirect_uri
 *  4. 浏览器跟随重定向回到 product-app 的 /oauth/callback 页面
 *
 * 为什么使用原生 form 而非 axios？
 *  Spring Auth Server 的 consent POST 端点返回的是 302 重定向，
 *  axios 会自动跟随重定向但可能丢失 session cookie；
 *  原生 form 提交浏览器会携带所有 cookie 并正确跟随 302，
 *  这是 OAuth2 规范要求的标准行为。
 */
import {ref, computed, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import axios from 'axios'

const AUTH_BASE = 'http://localhost:8080'

const route = useRoute()

/** 状态 */
const loading = ref(true)
const clientError = ref('')
const clientInfo = ref({})
const consentForm = ref(null)

/** Spring Auth Server 传递的参数（URL query） */
const clientId = route.query.client_id || ''
const state = route.query.state || ''
const rawScope = route.query.scope || ''

/**
 * 当前登录用户的昵称
 * 从 sessionStorage 读取（OAuthLogin.vue 在登录成功响应中写入）
 * 如果 sessionStorage 中没有，则回退到 "当前用户"
 */
const loginNickname = sessionStorage.getItem('oauth_login_nickname') || '当前用户'

/**
 * Scope 元数据：每个 scope 的显示名称、说明、图标
 * scope 值由 product-app 在 OAuth2 授权请求中指定（如 openid,profile,email）
 */
const SCOPE_META = {
  openid: {label: 'OpenID 身份', desc: '验证你的身份（必须）', icon: 'Key'},
  profile: {label: '基本资料', desc: '读取你的用户名、昵称和头像', icon: 'User'},
  email: {label: '邮箱地址', desc: '读取你的邮箱地址', icon: 'Message'},
  phone: {label: '手机号码', desc: '读取你的手机号码', icon: 'Phone'},
}

/** 权限列表：响应式，每个 scope 包含 value / label / desc / icon / approved（是否勾选） */
const scopeList = ref([])

/**
 * 当前已勾选的 scope 列表（用于动态生成 form hidden input）
 * 至少需要勾选 1 个 scope 才能提交授权
 */
const approvedScopes = computed(() =>
    scopeList.value.filter(s => s.approved).map(s => s.value)
)

/** 组件挂载时加载客户端信息 */
onMounted(async () => {
  if (!clientId) {
    clientError.value = '授权参数缺失（client_id 为空），请重新发起授权。'
    loading.value = false
    return
  }

  try {
    // 调用后端 /api/oauth2/client-info 获取应用的展示信息（名称/描述/Logo）
    // 同时验证应用是否存在且处于启用状态
    const res = await axios.get(`${AUTH_BASE}/api/oauth2/client-info`, {
      params: {clientId},
      withCredentials: true, // 携带 session cookie
    })
    const info = res.data

    if (!info.found || !info.enabled) {
      clientError.value = `应用「${clientId}」不存在或已被停用，无法完成授权。`
      loading.value = false
      return
    }
    clientInfo.value = info

    // 构建 scope 展示列表：解析 rawScope 字符串（空格分隔），为每个 scope 添加元数据
    const scopes = rawScope ? rawScope.split(' ').filter(Boolean) : []
    scopeList.value = scopes.map(v => ({
      value: v,
      label: SCOPE_META[v]?.label || v,
      desc: SCOPE_META[v]?.desc || '',
      icon: SCOPE_META[v]?.icon || 'Key',
      approved: true, // 默认全部勾选，用户可取消
    }))

  } catch (e) {
    clientError.value = '加载授权信息失败，请刷新页面重试。'
  } finally {
    loading.value = false
  }
})

/**
 * 用户点击"授权并继续"
 * 提交隐藏的 HTML form（POST 到 /oauth2/authorize）
 * Spring Auth Server 处理后重定向到 product-app 的 redirect_uri
 */
const handleApprove = () => {
  consentForm.value?.submit()
}

/** 用户点击"拒绝"：返回上一页（通常回到 product-app 登录页） */
const goBack = () => {
  window.history.back()
}
</script>

<style scoped>
/* ===================================================================
   全屏背景：浅灰蓝渐变
   =================================================================== */
.consent-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #f0f4f8 0%, #dce8f5 100%);
  padding: 24px;
}

/* ===================================================================
   授权确认卡片
   =================================================================== */
.consent-card {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 20px;
  padding: 32px 36px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.1);
}

/* 分割线 */
.divider {
  height: 1px;
  background: #f0f0f0;
  margin: 20px 0;
}

/* ===================================================================
   加载中 / 错误 状态块
   =================================================================== */
.status-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0 20px;
  gap: 12px;
}

.spin-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(102, 126, 234, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.spin-icon {
  color: #667eea;
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.error-icon-wrap {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(245, 108, 108, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #F56C6C;
}

.status-text {
  font-size: 15px;
  color: #606266;
  margin: 0;
  text-align: center;
}

.error-text {
  color: #F56C6C;
}

/* ===================================================================
   当前登录用户横幅
   =================================================================== */
.user-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.06));
  border-radius: 12px;
  border: 1px solid rgba(102, 126, 234, 0.15);
}

/* 用户头像：小圆形，紫色渐变 */
.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.user-greeting {
  font-size: 15px;
  color: #303133;
  line-height: 1.3;
}

.user-greeting strong {
  color: #667eea;
}

.user-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* ===================================================================
   应用信息区
   =================================================================== */
.app-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 4px 0 8px;
}

.app-logo {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.app-icon-default {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.app-name {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  text-align: center;
}

.app-desc {
  font-size: 13px;
  color: #909399;
  text-align: center;
  line-height: 1.5;
}

/* ===================================================================
   权限列表
   =================================================================== */
.scope-section {
  margin-bottom: 4px;
}

.scope-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
}

.scope-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 每个权限项：可点击，勾选时高亮 */
.scope-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.scope-item:hover {
  border-color: #c5caf5;
  background: #f5f7ff;
}

/* 已勾选状态：蓝紫色边框 */
.scope-item--checked {
  border-color: #c5caf5;
  background: rgba(102, 126, 234, 0.04);
}

/* 权限图标背景 */
.scope-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.scope-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.scope-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* ===================================================================
   操作按钮区
   =================================================================== */
.action-row {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin: 24px 0 16px;
}

/* 授权按钮：紫色渐变，与 OAuthLogin 保持一致 */
.approve-btn {
  min-width: 140px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  font-size: 15px;
  letter-spacing: 1px;
}

.approve-btn:hover {
  opacity: 0.9;
}

.approve-btn:disabled {
  opacity: 0.5;
}

/* 底部说明 */
.footer-note {
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  line-height: 1.6;
  margin: 0;
}
</style>

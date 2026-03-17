<template>
  <!-- ============================================================
       OAuth2 回调页（OAuthCallback）
       用途：用户在认证平台完成授权后，浏览器带 code + state 跳转到此页面。
       该页面自动处理授权码交换（登录 or 绑定），无需用户手动操作。
       布局：全屏居中的状态卡片（加载中 / pending / 成功 / 错误）
       ============================================================ -->
  <div class="callback-page">
    <div class="callback-card" :class="{ 'callback-card--wide': status === 'pending' }">

      <!-- ——— 加载中状态 ——— -->
      <div v-if="status === 'loading'" class="status-block">
        <!-- 旋转加载圈 -->
        <div class="spinner-ring">
          <el-icon :size="36" class="spinner-icon">
            <Loading/>
          </el-icon>
        </div>
        <p class="status-title">正在处理授权...</p>
        <p class="status-desc">请稍候，正在与认证平台完成交互</p>
      </div>

      <!-- ——— Pending 状态：首次 OAuth 登录，账号未绑定 ——— -->
      <div v-else-if="status === 'pending'" class="status-block pending-block">
        <div class="pending-header">
          <div class="pending-icon-wrap">
            <el-icon :size="32"><Connection/></el-icon>
          </div>
          <p class="status-title">账号关联</p>
          <p class="status-desc">您的 OAuth 账号尚未与本平台账号关联，请选择操作方式</p>
        </div>

        <!-- 两种处理方式的 Tab -->
        <el-tabs v-model="pendingTab" class="pending-tabs">

          <!-- Tab 1：快速注册新账号 -->
          <el-tab-pane label="快速注册" name="create">
            <div class="pending-pane">
              <p class="pane-desc">系统将根据您的 OAuth 信息自动创建一个新账号，无需填写额外信息。</p>
              <el-button
                  :loading="pendingLoading"
                  class="action-btn"
                  size="large"
                  type="primary"
                  @click="handleCreateNew"
              >
                立即创建新账号
              </el-button>
            </div>
          </el-tab-pane>

          <!-- Tab 2：绑定已有账号 -->
          <el-tab-pane label="绑定已有账号" name="bind">
            <div class="pending-pane">
              <p class="pane-desc">输入您已有的平台账号信息，将 OAuth 账号绑定到该账号。</p>
              <el-form
                  ref="bindFormRef"
                  :model="bindForm"
                  :rules="bindRules"
                  class="bind-form"
              >
                <el-form-item prop="username">
                  <el-input
                      v-model="bindForm.username"
                      clearable
                      placeholder="请输入用户名"
                      prefix-icon="User"
                      size="large"
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                      v-model="bindForm.password"
                      placeholder="请输入密码"
                      prefix-icon="Lock"
                      show-password
                      size="large"
                      type="password"
                      @keyup.enter="handleBindExisting"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button
                      :loading="pendingLoading"
                      class="action-btn"
                      size="large"
                      type="primary"
                      @click="handleBindExisting"
                  >
                    确认绑定
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

        </el-tabs>

        <!-- 返回登录 -->
        <div class="pending-footer">
          <el-link @click="$router.push('/login')">取消，返回登录页</el-link>
        </div>
      </div>

      <!-- ——— 成功状态 ——— -->
      <div v-else-if="status === 'success'" class="status-block">
        <div class="status-icon-wrap success">
          <el-icon :size="36">
            <CircleCheckFilled/>
          </el-icon>
        </div>
        <p class="status-title">授权成功</p>
        <p class="status-desc">{{ successMsg }}，即将自动跳转...</p>
      </div>

      <!-- ——— 错误状态 ——— -->
      <div v-else class="status-block">
        <div class="status-icon-wrap error">
          <el-icon :size="36">
            <CircleCloseFilled/>
          </el-icon>
        </div>
        <p class="status-title">授权失败</p>
        <p class="status-desc error-desc">{{ errorMsg }}</p>
        <el-button class="back-btn" type="primary" @click="$router.push('/login')">
          返回登录
        </el-button>
      </div>

    </div>
  </div>
</template>

<script setup>
/**
 * OAuth2 回调处理页 - 脚本逻辑
 *
 * 处理两种场景（由 sessionStorage.oauth_action 区分）：
 *
 *  1. 登录模式（action !== 'bind'）：
 *     调用 oauthCallback(code, state) → 后端用 code 换 Token
 *     → 正常登录：写入 Pinia + 跳转首页
 *     → pendingBind：展示 pending 状态，让用户选择创建新账号或绑定已有账号
 *
 *  2. 绑定模式（action === 'bind'）：
 *     调用 bindOAuth(code, state) → 绑定到当前已登录用户 → 跳转个人中心
 *
 * CSRF 防护：对比 URL 中的 state 参数与 sessionStorage 中存储的值。
 */
import {ref, reactive, onMounted} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {useUserStore} from '../../store/user'
import {oauthCallback, bindOAuth, oauthCreateNew, oauthBindExisting} from '../../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/**
 * 当前状态：'loading' | 'pending' | 'success' | 'error'
 * 控制页面显示哪个状态块
 */
const status = ref('loading')
const successMsg = ref('')
const errorMsg = ref('')

/** pending 状态下当前选中的 Tab */
const pendingTab = ref('create')

/** pending 状态下存储的 oauthPendingToken */
const pendingToken = ref('')

/** pending 操作的加载状态 */
const pendingLoading = ref(false)

/** 绑定已有账号的表单 ref 和数据 */
const bindFormRef = ref(null)
const bindForm = reactive({username: '', password: ''})
const bindRules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
}

/**
 * 登录成功的公共处理：写入 token + 跳转首页
 */
const handleLoginSuccess = (data, msg = '登录成功') => {
  userStore.setAuth(data)
  status.value = 'success'
  successMsg.value = msg
  setTimeout(() => router.push('/'), 1200)
}

/**
 * 快速注册新账号：调用 oauthCreateNew，成功后登录
 */
const handleCreateNew = async () => {
  pendingLoading.value = true
  try {
    const res = await oauthCreateNew({oauthPendingToken: pendingToken.value})
    handleLoginSuccess(res.data, '新账号创建并登录成功')
    ElMessage.success('新账号创建成功，已自动登录')
  } catch (e) {
    // 错误已由 axios 拦截器统一处理
  } finally {
    pendingLoading.value = false
  }
}

/**
 * 绑定已有账号：校验表单后调用 oauthBindExisting，成功后登录
 */
const handleBindExisting = async () => {
  const valid = await bindFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pendingLoading.value = true
  try {
    const res = await oauthBindExisting({
      oauthPendingToken: pendingToken.value,
      username: bindForm.username,
      password: bindForm.password,
    })
    handleLoginSuccess(res.data, 'OAuth 账号绑定并登录成功')
    ElMessage.success('账号绑定成功，已自动登录')
  } catch (e) {
    // 错误已由 axios 拦截器统一处理
  } finally {
    pendingLoading.value = false
  }
}

/** 组件挂载后立即处理 OAuth 回调参数 */
onMounted(async () => {
  // 从 URL query 参数中读取授权码和 state
  const code = route.query.code
  const state = route.query.state

  // 授权码缺失
  if (!code) {
    status.value = 'error'
    errorMsg.value = '授权码（code）缺失，请确认认证平台配置正确'
    return
  }

  // CSRF 验证：对比 state 参数
  const savedState = sessionStorage.getItem('oauth_state')
  if (savedState && state !== savedState) {
    status.value = 'error'
    errorMsg.value = 'State 参数不匹配，疑似 CSRF 攻击，请重新发起授权'
    return
  }

  // 清理 sessionStorage（一次性 state，防止重放攻击）
  sessionStorage.removeItem('oauth_state')
  const action = sessionStorage.getItem('oauth_action')
  sessionStorage.removeItem('oauth_action')

  try {
    if (action === 'bind') {
      // ── 绑定模式：将 OAuth 账号关联到当前登录的本地账号 ──
      const res = await bindOAuth({code, state})
      userStore.userInfo = res.data
      successMsg.value = '账号绑定成功'
      status.value = 'success'
      ElMessage.success('账号绑定成功')
      setTimeout(() => router.push('/profile'), 1200)

    } else {
      // ── 登录模式：用 OAuth 账号登录产品应用 ──
      const res = await oauthCallback({code, state})

      if (res.data.pendingBind === true) {
        // 首次登录，账号未绑定：进入 pending 选择状态
        pendingToken.value = res.data.oauthPendingToken
        status.value = 'pending'
      } else {
        // 正常登录成功
        handleLoginSuccess(res.data, 'OAuth2 登录成功')
        ElMessage.success('OAuth2 授权登录成功')
      }
    }

  } catch (e) {
    status.value = 'error'
    errorMsg.value = (action === 'bind' ? '账号绑定失败：' : '授权登录失败：') +
        (e?.response?.data?.message || e?.message || '未知错误，请重试')
  }
})
</script>

<style scoped>
/* ===================================================================
   全屏居中容器：与 Login.vue 保持一致的背景渐变
   =================================================================== */
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #0f7b6c 0%, #11998e 40%, #38ef7d 100%);
  padding: 20px;
}

/* ===================================================================
   状态卡片：白色背景，圆角阴影
   =================================================================== */
.callback-card {
  width: 380px;
  background: #fff;
  border-radius: 20px;
  padding: 48px 40px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  transition: width 0.3s ease;
}

/* pending 状态时卡片加宽 */
.callback-card--wide {
  width: 480px;
}

/* ===================================================================
   状态块通用：纵向 flex 居中
   =================================================================== */
.status-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

/* ——— 旋转加载动画圈 ——— */
.spinner-ring {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(17, 153, 142, 0.1), rgba(56, 239, 125, 0.15));
  border: 2px solid rgba(17, 153, 142, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

/* 图标旋转动画 */
.spinner-icon {
  color: #11998e;
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

/* ——— 成功/错误图标容器 ——— */
.status-icon-wrap {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.status-icon-wrap.success {
  background: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.status-icon-wrap.error {
  background: rgba(245, 108, 108, 0.1);
  color: #F56C6C;
}

/* ===================================================================
   Pending 状态块
   =================================================================== */
.pending-block {
  width: 100%;
  align-items: stretch;
  text-align: center;
}

.pending-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

/* pending 图标圆形背景 */
.pending-icon-wrap {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(17, 153, 142, 0.1), rgba(56, 239, 125, 0.15));
  border: 2px solid rgba(17, 153, 142, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #11998e;
  margin-bottom: 4px;
}

/* pending Tab 样式 */
.pending-tabs {
  width: 100%;
  text-align: left;
}

.pending-tabs :deep(.el-tabs__active-bar) {
  background-color: #11998e;
}

.pending-tabs :deep(.el-tabs__item.is-active) {
  color: #11998e;
}

.pending-tabs :deep(.el-tabs__item:hover) {
  color: #0f7b6c;
}

/* Tab 内容面板 */
.pending-pane {
  padding: 12px 0 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pane-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
  line-height: 1.6;
}

/* 绑定表单 */
.bind-form .el-form-item {
  margin-bottom: 16px;
}

/* 操作按钮：全宽青绿渐变 */
.action-btn {
  width: 100%;
  background: linear-gradient(135deg, #11998e, #38ef7d);
  border: none;
  font-size: 14px;
  letter-spacing: 1px;
}

.action-btn:hover {
  opacity: 0.9;
}

/* 底部取消链接 */
.pending-footer {
  margin-top: 8px;
  text-align: center;
}

/* ===================================================================
   文字样式
   =================================================================== */
.status-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.status-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
  line-height: 1.6;
}

.error-desc {
  color: #F56C6C;
}

/* ===================================================================
   返回登录按钮
   =================================================================== */
.back-btn {
  margin-top: 8px;
  background: linear-gradient(135deg, #11998e, #38ef7d);
  border: none;
  padding: 10px 28px;
}

.back-btn:hover {
  opacity: 0.9;
}
</style>

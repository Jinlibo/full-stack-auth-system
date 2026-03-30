<template>
  <div class="callback-page">
    <div class="orb orb-mint"></div>
    <div class="orb orb-sky"></div>

    <div class="callback-container">
      <!-- 左侧品牌面板 -->
      <div class="panel-left">
        <div class="brand-showcase">
          <div class="logo-box">
            <el-icon :size="40"><Box/></el-icon>
          </div>
          <h1 class="brand-title">微架构协同客户端</h1>
          <p class="brand-slogan">正在通过主站认证平台完成<br/>安全隧道授权验证</p>
          <div class="feature-pills">
            <span class="pill"><el-icon><Monitor/></el-icon> 统一安全身份体系</span>
            <span class="pill"><el-icon><Connection/></el-icon> 标准 OAuth2 内核</span>
            <span class="pill"><el-icon><DataLine/></el-icon> 账号多端平滑无缝融合</span>
          </div>
        </div>
      </div>

      <!-- 右侧状态区 -->
      <div class="panel-right">

        <!-- 加载中 -->
        <div v-if="status === 'loading'" class="status-block">
          <div class="spinner-ring">
            <el-icon :size="36" class="spinner-icon"><Loading/></el-icon>
          </div>
          <p class="status-title">正在处理授权...</p>
          <p class="status-desc">请稍候，正在与认证平台完成交互</p>
        </div>

        <!-- Pending：账号未绑定 -->
        <div v-else-if="status === 'pending'" class="status-block pending-block">
          <div class="pending-header">
            <div class="icon-circle">
              <el-icon :size="32"><Connection/></el-icon>
            </div>
            <p class="status-title">账号关联</p>
            <p class="status-desc">您的 OAuth 账号尚未与本平台账号关联，请选择操作方式</p>
          </div>

          <el-tabs v-model="pendingTab" class="pending-tabs">
            <el-tab-pane label="快速注册" name="create">
              <div class="pending-pane">
                <p class="pane-desc">系统将根据您的 OAuth 信息自动创建一个新账号，无需填写额外信息。</p>
                <el-button :loading="pendingLoading" class="grad-btn" size="large" type="primary" @click="handleCreateNew">
                  立即创建新账号
                </el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="绑定已有账号" name="bind">
              <div class="pending-pane">
                <p class="pane-desc">输入您已有的平台账号信息，将 OAuth 账号绑定到该账号。</p>
                <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules">
                  <el-form-item prop="username">
                    <el-input v-model="bindForm.username" clearable placeholder="请输入用户名" prefix-icon="User" size="large"/>
                  </el-form-item>
                  <el-form-item prop="password">
                    <el-input v-model="bindForm.password" placeholder="请输入密码" prefix-icon="Lock" show-password size="large" type="password" @keyup.enter="handleBindExisting"/>
                  </el-form-item>
                  <el-form-item>
                    <el-button :loading="pendingLoading" class="grad-btn" size="large" type="primary" @click="handleBindExisting">
                      确认绑定
                    </el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
          </el-tabs>

          <div class="pending-footer">
            <el-link @click="$router.push('/login')">取消，返回登录页</el-link>
          </div>
        </div>

        <!-- 成功 -->
        <div v-else-if="status === 'success'" class="status-block">
          <div class="icon-circle success">
            <el-icon :size="36"><CircleCheckFilled/></el-icon>
          </div>
          <p class="status-title">授权成功</p>
          <p class="status-desc">{{ successMsg }}，即将自动跳转...</p>
        </div>

        <!-- 错误 -->
        <div v-else class="status-block">
          <div class="icon-circle error">
            <el-icon :size="36"><CircleCloseFilled/></el-icon>
          </div>
          <p class="status-title">授权失败</p>
          <p class="status-desc error-text">{{ errorMsg }}</p>
          <el-button class="grad-btn" style="width:auto;padding:0 28px;margin-top:8px" type="primary" @click="$router.push('/login')">
            返回登录
          </el-button>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * OAuth2 回调处理页
 *
 * 场景一（登录）：oauthCallback → 正常登录 | pendingBind（未绑定账号）
 * 场景二（绑定）：bindOAuth → 绑定当前已登录用户 → 跳转个人中心
 * CSRF 防护：URL state 与 sessionStorage 中的值对比
 */
import {ref, reactive, onMounted} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {useUserStore} from '../../store/user'
import {oauthCallback, bindOAuth, oauthCreateNew, oauthBindExisting} from '../../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const status = ref('loading')   // 'loading' | 'pending' | 'success' | 'error'
const successMsg = ref('')
const errorMsg = ref('')
const pendingTab = ref('create')
const pendingToken = ref('')
const pendingLoading = ref(false)
const bindFormRef = ref(null)
const bindForm = reactive({username: '', password: ''})
const bindRules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
}

const handleLoginSuccess = (data, msg = '登录成功') => {
  userStore.setAuth(data)
  successMsg.value = msg
  status.value = 'success'
  setTimeout(() => router.push('/'), 1200)
}

const handleCreateNew = async () => {
  pendingLoading.value = true
  try {
    const res = await oauthCreateNew({oauthPendingToken: pendingToken.value})
    handleLoginSuccess(res.data, '新账号创建并登录成功')
    ElMessage.success('新账号创建成功，已自动登录')
  } catch {
    // 错误由 axios 拦截器统一处理
  } finally {
    pendingLoading.value = false
  }
}

const handleBindExisting = async () => {
  if (!await bindFormRef.value?.validate().catch(() => false)) return
  pendingLoading.value = true
  try {
    const res = await oauthBindExisting({
      oauthPendingToken: pendingToken.value,
      ...bindForm,
    })
    handleLoginSuccess(res.data, 'OAuth 账号绑定并登录成功')
    ElMessage.success('账号绑定成功，已自动登录')
  } catch {
    // 错误由 axios 拦截器统一处理
  } finally {
    pendingLoading.value = false
  }
}

onMounted(async () => {
  const {code, state} = route.query

  if (!code) {
    status.value = 'error'
    errorMsg.value = '授权码（code）缺失，请确认认证平台配置正确'
    return
  }

  const savedState = sessionStorage.getItem('oauth_state')
  if (savedState && state !== savedState) {
    status.value = 'error'
    errorMsg.value = 'State 参数不匹配，疑似 CSRF 攻击，请重新发起授权'
    return
  }

  sessionStorage.removeItem('oauth_state')
  const action = sessionStorage.getItem('oauth_action')
  sessionStorage.removeItem('oauth_action')

  try {
    if (action === 'bind') {
      const res = await bindOAuth({code, state})
      userStore.userInfo = res.data
      successMsg.value = '账号绑定成功'
      status.value = 'success'
      ElMessage.success('账号绑定成功')
      setTimeout(() => router.push('/profile'), 1200)
    } else {
      const res = await oauthCallback({code, state})
      if (res.data.pendingBind) {
        pendingToken.value = res.data.oauthPendingToken
        status.value = 'pending'
      } else {
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
/* ── 全屏页面容器 ── */
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  padding: 20px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}

/* 背景光球 */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  animation: float 25s infinite ease-in-out alternate;
  pointer-events: none;
  z-index: 0;
}
.orb-mint { width: 50vw; height: 50vw; background: rgba(52, 211, 153, 0.4); top: -10vw; left: -10vw; }
.orb-sky  { width: 45vw; height: 45vw; background: rgba(56, 189, 248, 0.4); bottom: -10vw; right: -5vw; animation-delay: -5s; }
@keyframes float {
  from { transform: translateY(0) scale(1); }
  to   { transform: translateY(-40px) scale(1.1); }
}

/* ── 两栏玻璃卡片容器 ── */
.callback-container {
  width: 1000px;
  max-width: 95vw;
  min-height: 600px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-radius: 28px;
  box-shadow: 0 40px 100px -20px rgba(0, 0, 0, 0.1), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  display: flex;
  overflow: hidden;
  position: relative;
  z-index: 10;
}

/* ── 左侧品牌面板 ── */
.panel-left {
  flex: 5;
  background: linear-gradient(145deg, rgba(20, 184, 166, 0.95) 0%, rgba(14, 165, 233, 0.9) 100%);
  padding: 60px;
  display: flex;
  align-items: center;
  position: relative;
  color: #fff;
}
.panel-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255,255,255,0.2) 1px, transparent 1px);
  background-size: 20px 20px;
  opacity: 0.3;
}

.brand-showcase { position: relative; z-index: 2; width: 100%; }

.logo-box {
  width: 72px; height: 72px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  margin-bottom: 30px;
}

.brand-title  { font-size: 32px; font-weight: 800; margin: 0 0 16px; letter-spacing: 0.5px; }
.brand-slogan { font-size: 15px; opacity: 0.9; margin: 0 0 40px; line-height: 1.7; }

.feature-pills { display: flex; flex-direction: column; gap: 16px; }
.pill {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  backdrop-filter: blur(10px);
  transition: all 0.3s;
  width: fit-content;
}
.pill:hover { background: rgba(255, 255, 255, 0.25); transform: translateX(6px); }

/* ── 右侧状态区 ── */
.panel-right {
  flex: 6;
  padding: 40px 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ccfbf1 0%, #bae6fd 100%);
}

/* ── 状态块（loading / success / error 共用） ── */
.status-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 340px;
  text-align: center;
}

/* pending 状态稍宽 */
.pending-block { max-width: 420px; align-items: stretch; }

/* ── 图标圆圈（loading / pending / success / error 统一用此类） ── */
.icon-circle {
  width: 80px; height: 80px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 4px;
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.1), rgba(14, 165, 233, 0.15));
  border: 2px solid rgba(20, 184, 166, 0.2);
  color: #14b8a6;
}
.icon-circle.success { background: rgba(103, 194, 58, 0.1); border-color: transparent; color: #67C23A; }
.icon-circle.error   { background: rgba(245, 108, 108, 0.1); border-color: transparent; color: #F56C6C; }

/* loading spinner */
.spinner-ring {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.1), rgba(14, 165, 233, 0.15));
  border: 2px solid rgba(20, 184, 166, 0.2);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 4px;
}
.spinner-icon {
  color: #14b8a6;
  animation: spin 1.2s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── Pending 专属 ── */
.pending-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.pending-tabs { width: 100%; text-align: left; }
.pending-tabs :deep(.el-tabs__active-bar) { background-color: #14b8a6; }
.pending-tabs :deep(.el-tabs__item.is-active) { color: #14b8a6; }
.pending-tabs :deep(.el-tabs__item:hover) { color: #0d9488; }

.pending-pane { padding: 12px 0 4px; display: flex; flex-direction: column; gap: 12px; }
.pane-desc { font-size: 13px; color: #64748b; margin: 0; line-height: 1.6; }
.pending-footer { margin-top: 8px; text-align: center; }

/* ── 通用文字 ── */
.status-title { font-size: 18px; font-weight: 700; color: #0f172a; margin: 0; }
.status-desc  { font-size: 14px; color: #64748b; margin: 0; line-height: 1.6; }
.error-text   { color: #F56C6C; }

/* ── 渐变按钮（action-btn / back-btn 合并为一个类） ── */
.grad-btn {
  width: 100%;
  background: linear-gradient(135deg, #14b8a6, #0ea5e9) !important;
  border: none !important;
  color: #fff !important;
  font-size: 14px;
  letter-spacing: 1px;
}
.grad-btn:hover { opacity: 0.9; }

/* 响应式 */
@media (max-width: 900px) {
  .panel-left { display: none; }
  .callback-container { min-height: auto; }
  .panel-right { padding: 40px 30px; }
}
</style>

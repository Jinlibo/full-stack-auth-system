<template>
  <!-- ============================================================
       OAuth2 回调页（OAuthCallback）
       用途：用户在认证平台完成授权后，浏览器带 code + state 跳转到此页面。
       该页面自动处理授权码交换（登录 or 绑定），无需用户手动操作。
       布局：全屏居中的状态卡片（加载中 / 成功 / 错误）
       ============================================================ -->
  <div class="callback-page">
    <div class="callback-card">

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
 *     调用 oauthCallback(code, state) → 后端用 code 换 Token，创建或匹配本地用户
 *     → 成功：写入 Pinia token + userInfo → 跳转首页
 *
 *  2. 绑定模式（action === 'bind'）：
 *     调用 bindOAuth(code, state) → 后端将 OAuth 账号绑定到当前已登录用户
 *     → 成功：更新 Pinia userInfo → 跳转个人中心页
 *
 * CSRF 防护：
 *  对比 URL 中的 state 参数与 sessionStorage 中存储的值，不一致则拒绝处理。
 *  sessionStorage 的生命周期仅限当前 Tab，防止跨 Tab 的 CSRF 攻击。
 */
import {ref, onMounted} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {useUserStore} from '../../store/user'
import {oauthCallback, bindOAuth} from '../../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/**
 * 当前状态：'loading' | 'success' | 'error'
 * 控制页面显示哪个状态块
 */
const status = ref('loading')
const successMsg = ref('')
const errorMsg = ref('')

/** 组件挂载后立即处理 OAuth 回调参数 */
onMounted(async () => {
  // 从 URL query 参数中读取授权码和 state
  const code = route.query.code
  const state = route.query.state

  // 授权码缺失：认证平台可能拒绝了授权或配置错误
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
      // 直接更新 userInfo（后端返回最新的用户信息含绑定列表）
      userStore.userInfo = res.data
      successMsg.value = '账号绑定成功'
      status.value = 'success'
      ElMessage.success('账号绑定成功')

      // 1.2 秒后自动跳转到个人中心页
      setTimeout(() => router.push('/profile'), 1200)

    } else {
      // ── 登录模式：用 OAuth 账号登录产品应用 ──
      const res = await oauthCallback({code, state})
      // setAuth：写入 token + userInfo 到 Pinia store（含 localStorage 持久化）
      userStore.setAuth(res.data)
      successMsg.value = 'OAuth2 登录成功'
      status.value = 'success'
      ElMessage.success('OAuth2 授权登录成功')

      // 1.2 秒后自动跳转到首页
      setTimeout(() => router.push('/'), 1200)
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

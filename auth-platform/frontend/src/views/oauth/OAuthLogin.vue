<template>
  <!-- ============================================================
       OAuth2 授权登录页（OAuthLogin）
       触发时机：product-app 发起 OAuth2 授权码流程时，
                 认证平台检测到用户未登录，将浏览器重定向到此页面。
       登录方式：Spring Security 标准 form login（POST /login，application/x-www-form-urlencoded）
                  使用 session cookie 而非 JWT（授权服务器需要服务端 session 跟踪登录状态）
       登录成功：后端返回 redirectUrl（原来的 OAuth2 授权地址）+ 用户昵称，
                  前端存储昵称到 sessionStorage 后跳转，OAuthConsent 页面读取展示
       ============================================================ -->
  <div class="oauth-login-bg">
    <!-- 装饰光圈 -->
    <div class="bg-orb bg-orb--1"></div>
    <div class="bg-orb bg-orb--2"></div>

    <div class="login-card">
      <!-- 卡片头部：平台图标 + 标题 -->
      <div class="card-header">
        <div class="platform-icon">
          <el-icon :size="28">
            <Lock/>
          </el-icon>
        </div>
        <div class="platform-title">认证授权平台</div>
        <div class="platform-sub">请登录后继续授权操作</div>
      </div>

      <!-- 登录表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              placeholder="用户名"
              prefix-icon="User"
              size="large"
              autocomplete="username"
              clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              autocomplete="current-password"
              placeholder="密码"
              prefix-icon="Lock"
              size="large"
              show-password
              type="password"
              @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              size="large"
              @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 错误提示 -->
      <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          :closable="false"
          show-icon
          style="margin-top: 4px;"
      />

      <!-- 说明提示 -->
      <p class="hint-text">
        <el-icon style="vertical-align: -2px;">
          <InfoFilled/>
        </el-icon>
        登录成功后将跳转至授权确认页
      </p>
    </div>
  </div>
</template>

<script setup>
/**
 * OAuth2 授权登录页 - 脚本逻辑
 *
 * 关键点：此页面使用 Spring Security form login（非 JWT 登录）
 *
 * 技术细节：
 *  - POST Content-Type: application/x-www-form-urlencoded（必须，Spring Security 要求）
 *  - withCredentials: true（必须，让浏览器携带并保存 session cookie）
 *  - 成功响应包含 { redirectUrl, nickname, username }
 *  - 将 nickname/username 存入 sessionStorage，OAuthConsent 页面读取展示
 *  - 登录成功后通过 window.location.href 跳转（携带 session cookie）
 *
 * 与管理后台登录的区别：
 *  - 管理后台：POST /api/auth/login → JWT token → 无 session
 *  - 此页面：POST /login → session cookie → 无 JWT
 */
import {ref, reactive} from 'vue'
import axios from 'axios'

/** 认证平台后端地址（固定，此页面独立于前端路由状态） */
const AUTH_BASE = 'http://localhost:8080'

const formRef = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({username: '', password: ''})

const rules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
}

/**
 * 执行 OAuth2 表单登录
 *
 * 注意事项：
 *  1. 必须以 application/x-www-form-urlencoded 格式 POST 到 /login，
 *     才能被 Spring Security 的 UsernamePasswordAuthenticationFilter 识别
 *  2. withCredentials=true 让浏览器在 POST 请求中发送 cookie，
 *     同时在响应成功后保存 Set-Cookie 中的 JSESSIONID
 *  3. 成功后 window.location.href 跳转（而非 router.push），
 *     确保 session cookie 随跳转请求发出，让 Spring Auth Server 能识别已登录状态
 */
const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  errorMsg.value = ''

  try {
    // URLSearchParams 自动生成 application/x-www-form-urlencoded 格式
    const params = new URLSearchParams()
    params.append('username', form.username)
    params.append('password', form.password)

    const res = await axios.post(`${AUTH_BASE}/login`, params, {
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      withCredentials: true, // 携带+保存 session cookie，授权服务器需要此 cookie 识别登录状态
    })

    // 后端 successHandler 返回：{ redirectUrl, nickname, username }
    // 将用户昵称存入 sessionStorage，供 OAuthConsent 页面在授权确认时展示
    const nickname = res.data?.nickname || res.data?.username || form.username
    sessionStorage.setItem('oauth_login_nickname', nickname)

    // 跳转到 OAuth2 授权地址（通常是 /oauth2/authorize?...，
    // 浏览器会携带 session cookie，Spring Auth Server 识别为已登录用户）
    const redirectUrl = res.data?.redirectUrl || AUTH_BASE
    window.location.href = redirectUrl

  } catch (e) {
    // failureHandler 返回 { message: "Bad credentials" }，这里翻译为友好提示
    const rawMsg = e.response?.data?.message || ''
    errorMsg.value = rawMsg.toLowerCase().includes('bad credentials') || rawMsg.includes('不存在') || rawMsg.includes('禁用')
        ? '用户名或密码错误，请重新输入'
        : (rawMsg || '登录失败，请检查网络或稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===================================================================
   全屏背景：蓝紫渐变
   =================================================================== */
.oauth-login-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #1a1c2e 0%, #2d2f4a 50%, #1e3a5f 100%);
  position: relative;
  overflow: hidden;
  padding: 24px;
}

/* 装饰光圈：大型模糊圆形 */
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.2;
  pointer-events: none;
}

.bg-orb--1 {
  width: 500px;
  height: 500px;
  background: #667eea;
  top: -200px;
  left: -100px;
  animation: drift 12s ease-in-out infinite alternate;
}

.bg-orb--2 {
  width: 400px;
  height: 400px;
  background: #764ba2;
  bottom: -150px;
  right: -80px;
  animation: drift 16s ease-in-out infinite alternate-reverse;
}

@keyframes drift {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(30px, 20px) scale(1.08);
  }
}

/* ===================================================================
   登录卡片：毛玻璃效果
   =================================================================== */
.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 40px 40px 32px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.4);
}

/* 卡片头部 */
.card-header {
  text-align: center;
  margin-bottom: 32px;
}

/* 平台图标：圆形渐变背景 */
.platform-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.platform-title {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 6px;
}

.platform-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}

/* ===================================================================
   表单样式：输入框深色毛玻璃
   =================================================================== */
.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none !important;
}

.login-form :deep(.el-input__wrapper:hover),
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  background: rgba(255, 255, 255, 0.12);
}

.login-form :deep(.el-input__inner) {
  color: #fff;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35);
}

.login-form :deep(.el-input__prefix-inner .el-icon),
.login-form :deep(.el-input__suffix-inner .el-icon) {
  color: rgba(255, 255, 255, 0.45);
}

.login-form :deep(.el-form-item__error) {
  color: #ff9f7f;
}

/* 登录按钮：全宽，紫色渐变 */
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border: none;
  border-radius: 8px;
  transition: opacity 0.2s, transform 0.15s;
}

.login-btn:hover {
  opacity: 0.88;
  transform: translateY(-1px);
}

/* 底部提示文字 */
.hint-text {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
  margin: 16px 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
</style>

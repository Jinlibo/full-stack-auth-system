<template>
  <!-- ============================================================
       登录页（Login）
       布局：左侧品牌展示区 + 右侧登录表单区
       支持两种登录方式：账号密码 / OAuth2 认证平台授权
       ============================================================ -->
  <div class="login-page">

    <!-- ——— 左侧：品牌装饰区 ——— -->
    <div class="brand-panel">
      <!-- 装饰性圆形光晕，增加视觉层次感 -->
      <div class="brand-orb brand-orb--1"></div>
      <div class="brand-orb brand-orb--2"></div>

      <!-- 品牌 Logo 与标题 -->
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon :size="40">
            <Box/>
          </el-icon>
        </div>
        <h1 class="brand-title">产品应用平台</h1>
        <p class="brand-subtitle">基于 OAuth2 的现代化产品服务</p>

        <!-- 功能亮点列表 -->
        <ul class="brand-features">
          <li>
            <el-icon>
              <Check/>
            </el-icon>
            <span>统一身份认证，安全可靠</span>
          </li>
          <li>
            <el-icon>
              <Check/>
            </el-icon>
            <span>OAuth2 授权码模式，一键登录</span>
          </li>
          <li>
            <el-icon>
              <Check/>
            </el-icon>
            <span>多账号绑定，灵活管理</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- ——— 右侧：登录表单区 ——— -->
    <div class="form-panel">
      <div class="form-box">
        <!-- 页头：标题 + 副标题 -->
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>请选择登录方式</p>
        </div>

        <!-- 登录方式切换 Tab -->
        <el-tabs v-model="activeTab" class="login-tabs">

          <!-- ── Tab 1：账号密码登录 ── -->
          <el-tab-pane label="账号密码登录" name="password">
            <el-form
                ref="formRef"
                :model="form"
                :rules="rules"
                class="login-form"
                @submit.prevent="handleLogin"
            >
              <!-- 用户名输入框 -->
              <el-form-item prop="username">
                <el-input
                    v-model="form.username"
                    clearable
                    placeholder="请输入用户名"
                    prefix-icon="User"
                    size="large"
                />
              </el-form-item>

              <!-- 密码输入框（show-password 显示密码眼睛图标） -->
              <el-form-item prop="password">
                <el-input
                    v-model="form.password"
                    placeholder="请输入密码"
                    prefix-icon="Lock"
                    show-password
                    size="large"
                    type="password"
                    @keyup.enter="handleLogin"
                />
              </el-form-item>

              <!-- 提交按钮 -->
              <el-form-item>
                <el-button
                    :loading="loading"
                    class="submit-btn"
                    size="large"
                    type="primary"
                    @click="handleLogin"
                >
                  {{ loading ? '登录中...' : '登 录' }}
                </el-button>
              </el-form-item>
            </el-form>

            <!-- 默认账号提示 -->
            <p class="hint-text">默认账号：admin / admin123</p>

            <!-- 注册链接 -->
            <p class="register-link">还没有账号？<el-link type="primary" @click="$router.push('/register')">立即注册</el-link></p>
          </el-tab-pane>

          <!-- ── Tab 2：OAuth2 第三方授权登录 ── -->
          <el-tab-pane label="授权登录" name="oauth">
            <div class="oauth-panel">
              <div class="oauth-icon-wrap">
                <el-icon :size="48">
                  <Connection/>
                </el-icon>
              </div>
              <p class="oauth-desc">使用认证平台账号进行 OAuth2 授权登录</p>

              <!-- 授权按钮 -->
              <el-button
                  :loading="oauthLoading"
                  class="oauth-btn"
                  size="large"
                  type="primary"
                  @click="handleOAuthLogin"
              >
                <el-icon v-if="!oauthLoading">
                  <Link/>
                </el-icon>
                认证平台授权登录
              </el-button>

              <p class="oauth-tip">
                点击后将跳转到认证授权平台完成登录，授权完成后自动返回
              </p>
            </div>
          </el-tab-pane>

        </el-tabs>
      </div>
    </div>

  </div>
</template>

<script setup>
/**
 * 登录页 - 脚本逻辑
 *
 * 支持两种登录方式：
 *  1. 账号密码登录：调用 userStore.login → 后端颁发 JWT → 跳转首页
 *  2. OAuth2 授权登录：调用 getOAuthUrl 获取授权地址 → 重定向到认证平台 →
 *     用户授权后跳回 /oauth/callback（由 OAuthCallback.vue 处理）
 *
 * CSRF 防护：
 *  OAuth2 state 参数存入 sessionStorage，OAuthCallback.vue 对比验证，
 *  防止跨站请求伪造攻击（CSRF）。
 */
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {useUserStore} from '../../store/user'
import {getOAuthUrl} from '../../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

/** 当前激活的登录 Tab（'password' | 'oauth'） */
const activeTab = ref('password')

/** el-form 的 ref，用于调用 validate() 进行表单校验 */
const formRef = ref(null)

/** 账号密码登录按钮加载状态 */
const loading = ref(false)

/** OAuth 跳转按钮加载状态（获取授权 URL 期间） */
const oauthLoading = ref(false)

/** 表单数据（双向绑定到 el-input） */
const form = reactive({username: '', password: ''})

/** el-form 校验规则 */
const rules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
}

/**
 * 账号密码登录处理
 * 1. 触发表单校验，不通过则阻止提交
 * 2. 调用 userStore.login（内部调用 /api/auth/login，成功后写入 token + userInfo）
 * 3. 跳转到首页
 */
const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // 错误已由 request.js axios 拦截器统一处理（ElMessage.error）
  } finally {
    loading.value = false
  }
}

/**
 * OAuth2 授权登录处理
 * 1. 生成随机 state 字符串，存入 sessionStorage（OAuthCallback.vue 验证用）
 * 2. 调用 getOAuthUrl 获取授权 URL（后端拼接 client_id、redirect_uri、scope、state）
 * 3. 浏览器直接跳转到认证平台授权页（window.location.href）
 */
const handleOAuthLogin = async () => {
  oauthLoading.value = true
  try {
    // 随机 state：用于防 CSRF，OAuthCallback.vue 会对比该值与回调参数中的 state
    const state = Math.random().toString(36).substring(7)
    sessionStorage.setItem('oauth_state', state)
    const res = await getOAuthUrl(state)
    // 重定向到认证平台，用户在认证平台完成登录授权后，
    // 认证平台会带着 code + state 回调到 redirect-uri（/oauth/callback）
    window.location.href = res.data.authorizeUrl
  } catch (e) {
    ElMessage.error('获取授权地址失败，请检查认证平台是否正常运行')
  } finally {
    oauthLoading.value = false
  }
}
</script>

<style scoped>
/* ===================================================================
   登录页整体布局：左右两栏，100vh 全屏
   =================================================================== */
.login-page {
  display: flex;
  min-height: 100vh;
}

/* ===================================================================
   左侧品牌展示面板：青绿色渐变背景，宽 45%
   =================================================================== */
.brand-panel {
  position: relative;
  width: 45%;
  background: linear-gradient(145deg, #0f7b6c 0%, #11998e 40%, #38ef7d 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  /* 移动端隐藏，仅中屏及以上显示 */
}

/* 装饰性光晕：绝对定位的大圆圈，增加视觉层次 */
.brand-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.brand-orb--1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
}

.brand-orb--2 {
  width: 300px;
  height: 300px;
  bottom: -80px;
  left: -80px;
}

/* 品牌内容区：相对定位，确保在光晕层之上 */
.brand-content {
  position: relative;
  z-index: 1;
  color: #fff;
  padding: 40px;
  max-width: 380px;
}

/* 品牌 Logo 圆形背景 */
.brand-logo {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  backdrop-filter: blur(4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 10px;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 15px;
  opacity: 0.85;
  margin: 0 0 32px;
  line-height: 1.6;
}

/* 功能列表：无样式列表，每项带绿色勾选图标 */
.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.brand-features li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  opacity: 0.9;
}

.brand-features .el-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
}

/* ===================================================================
   右侧表单面板：白色背景，居中对齐
   =================================================================== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px 20px;
}

/* 表单卡片容器，最大宽度 420px */
.form-box {
  width: 100%;
  max-width: 420px;
}

/* 表单头部：大标题 + 副标题 */
.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.form-header p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 登录 Tab 整体样式覆写 */
.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.login-tabs :deep(.el-tabs__active-bar) {
  background-color: #11998e;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #11998e;
}

.login-tabs :deep(.el-tabs__item:hover) {
  color: #0f7b6c;
}

/* 表单项间距 */
.login-form .el-form-item {
  margin-bottom: 20px;
}

/* 登录按钮：全宽，青绿色渐变背景 */
.submit-btn {
  width: 100%;
  background: linear-gradient(135deg, #11998e, #38ef7d);
  border: none;
  font-size: 15px;
  letter-spacing: 2px;
  transition: opacity 0.2s, transform 0.15s;
}

.submit-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* 默认账号提示文字 */
.hint-text {
  text-align: center;
  font-size: 13px;
  color: #c0c4cc;
  margin: 4px 0 0;
}

/* 注册链接 */
.register-link {
  text-align: center;
  font-size: 13px;
  color: #909399;
  margin: 8px 0 0;
}

/* ===================================================================
   OAuth2 授权登录面板
   =================================================================== */
.oauth-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0 8px;
  text-align: center;
}

/* OAuth 图标圆形背景 */
.oauth-icon-wrap {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(17, 153, 142, 0.1), rgba(56, 239, 125, 0.15));
  border: 2px solid rgba(17, 153, 142, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #11998e;
  margin-bottom: 20px;
}

.oauth-desc {
  font-size: 14px;
  color: #606266;
  margin: 0 0 24px;
  line-height: 1.6;
}

/* OAuth 授权按钮：宽度 80%，同样青绿渐变 */
.oauth-btn {
  width: 80%;
  background: linear-gradient(135deg, #11998e, #38ef7d);
  border: none;
  font-size: 15px;
  letter-spacing: 1px;
  margin-bottom: 16px;
}

.oauth-btn:hover {
  opacity: 0.9;
}

.oauth-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin: 0;
  line-height: 1.6;
  max-width: 300px;
}

/* ===================================================================
   响应式：移动端隐藏左侧品牌栏
   =================================================================== */
@media (max-width: 768px) {
  .brand-panel {
    display: none;
  }
}
</style>

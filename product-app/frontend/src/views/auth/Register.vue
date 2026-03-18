<template>
  <!-- ============================================================
       注册页（Register）
       布局：左侧品牌展示区（与 Login.vue 一致） + 右侧注册表单区
       ============================================================ -->
  <div class="register-page">

    <!-- ——— 左侧：品牌装饰区 ——— -->
    <div class="brand-panel">
      <!-- 装饰性圆形光晕 -->
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
        <p class="brand-subtitle">创建您的账号，开始使用产品服务</p>

        <!-- 功能亮点列表 -->
        <ul class="brand-features">
          <li>
            <el-icon><Check/></el-icon>
            <span>快速注册，立即使用</span>
          </li>
          <li>
            <el-icon><Check/></el-icon>
            <span>支持 OAuth2 第三方授权绑定</span>
          </li>
          <li>
            <el-icon><Check/></el-icon>
            <span>安全加密，保护您的数据</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- ——— 右侧：注册表单区 ——— -->
    <div class="form-panel">
      <div class="form-box">
        <!-- 页头 -->
        <div class="form-header">
          <h2>创建账号</h2>
          <p>填写以下信息完成注册</p>
        </div>

        <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            class="register-form"
            label-position="top"
        >
          <!-- 用户名 -->
          <el-form-item prop="username" label="用户名">
            <el-input
                v-model="form.username"
                clearable
                placeholder="请输入用户名"
                prefix-icon="User"
                size="large"
            />
          </el-form-item>

          <!-- 密码 -->
          <el-form-item prop="password" label="密码">
            <el-input
                v-model="form.password"
                placeholder="请输入密码（至少6位）"
                prefix-icon="Lock"
                show-password
                size="large"
                type="password"
            />
          </el-form-item>

          <!-- 确认密码 -->
          <el-form-item prop="confirmPassword" label="确认密码">
            <el-input
                v-model="form.confirmPassword"
                placeholder="请再次输入密码"
                prefix-icon="Lock"
                show-password
                size="large"
                type="password"
                @keyup.enter="handleRegister"
            />
          </el-form-item>

          <!-- 邮箱 -->
          <el-form-item prop="email" label="邮箱">
            <el-input
                v-model="form.email"
                clearable
                placeholder="请输入邮箱地址"
                prefix-icon="Message"
                size="large"
            />
          </el-form-item>

          <!-- 昵称（可选） -->
          <el-form-item prop="nickname" label="昵称（可选）">
            <el-input
                v-model="form.nickname"
                clearable
                placeholder="请输入昵称，不填则与用户名相同"
                prefix-icon="EditPen"
                size="large"
            />
          </el-form-item>

          <!-- 手机号（可选） -->
          <el-form-item prop="phone" label="手机号（可选）">
            <el-input
                v-model="form.phone"
                clearable
                placeholder="请输入手机号"
                prefix-icon="Phone"
                size="large"
            />
          </el-form-item>

          <!-- 提交按钮 -->
          <el-form-item>
            <el-button
                :loading="loading"
                class="submit-btn"
                size="large"
                type="primary"
                @click="handleRegister"
            >
              {{ loading ? '注册中...' : '立即注册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 已有账号跳转 -->
        <p class="login-link">
          已有账号？
          <el-link type="primary" @click="$router.push('/login')">立即登录</el-link>
        </p>
      </div>
    </div>

  </div>
</template>

<script setup>
/**
 * 注册页 - 脚本逻辑
 *
 * 表单校验：用户名必填、密码必填且至少6位、确认密码一致、邮箱格式正确
 * 提交成功后提示"注册成功，请登录"并跳转到登录页
 */
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {register} from '../../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()

/** el-form 的 ref，用于调用 validate() */
const formRef = ref(null)

/** 注册按钮加载状态 */
const loading = ref(false)

/** 表单数据 */
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: '',
  phone: '',
})

/** 确认密码校验器 */
const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/** 表单校验规则 */
const rules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 2, max: 20, message: '用户名长度为 2~20 个字符', trigger: 'blur'},
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少6位', trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, validator: validateConfirmPassword, trigger: 'blur'},
  ],
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '请输入正确的邮箱格式', trigger: ['blur', 'change']},
  ],
}

/**
 * 提交注册
 * 1. 表单校验
 * 2. 调用 register API（不含 confirmPassword 字段）
 * 3. 成功后提示并跳转登录页
 */
const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const {confirmPassword, ...payload} = form
    // 昵称为空时不传字段
    if (!payload.nickname) delete payload.nickname
    // 手机号为空时不传字段
    if (!payload.phone) delete payload.phone
    await register(payload)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // 错误已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===================================================================
   注册页整体布局：左右两栏，100vh 全屏（与 Login.vue 一致）
   =================================================================== */
.register-page {
  display: flex;
  min-height: 100vh;
}

/* ===================================================================
   左侧品牌面板：青绿色渐变背景，宽 45%
   =================================================================== */
.brand-panel {
  position: relative;
  width: 45%;
  background: linear-gradient(145deg, #0f7b6c 0%, #11998e 40%, #38ef7d 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 装饰性光晕 */
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

/* 品牌内容区 */
.brand-content {
  position: relative;
  z-index: 1;
  color: #fff;
  padding: 40px;
  max-width: 380px;
}

/* Logo 图标背景 */
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

/* 功能列表 */
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
  overflow-y: auto;
}

/* 表单卡片容器 */
.form-box {
  width: 100%;
  max-width: 420px;
}

/* 表单头部 */
.form-header {
  margin-bottom: 28px;
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

/* 表单项间距 */
.register-form .el-form-item {
  margin-bottom: 18px;
}

/* 注册按钮：全宽，青绿色渐变 */
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

/* 跳转登录链接 */
.login-link {
  text-align: center;
  font-size: 13px;
  color: #909399;
  margin: 8px 0 0;
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

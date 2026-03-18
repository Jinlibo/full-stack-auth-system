<template>
  <!-- ============================================================
       个人中心页（Profile）
       功能：
         1. 个人资料编辑（昵称、邮箱、手机号）
         2. 第三方账号绑定管理（绑定/解绑 OAuth2 认证平台账号）
       布局：顶部个人横幅 + 下方两列卡片
       ============================================================ -->
  <div class="profile-page">

    <!-- ——— 个人横幅：头像 + 显示名 + 基础信息 ——— -->
    <div class="profile-banner">
      <div class="banner-orb banner-orb--1"></div>
      <div class="banner-orb banner-orb--2"></div>

      <div class="banner-inner">
        <!-- 大头像：显示昵称首字母 -->
        <el-avatar :size="72" class="banner-avatar">
          {{ (userStore.userInfo?.nickname || userStore.userInfo?.username)?.charAt(0)?.toUpperCase() }}
        </el-avatar>

        <!-- 用户名 + 邮箱信息 -->
        <div class="banner-info">
          <h2 class="banner-name">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
          </h2>
          <div class="banner-meta">
            <span v-if="userStore.userInfo?.email">
              <el-icon style="vertical-align: -2px;"><Message/></el-icon>
              {{ userStore.userInfo.email }}
            </span>
          </div>
          <!-- 角色标签（半透明白色） -->
          <div class="banner-roles">
            <el-tag
                v-for="r in userStore.userInfo?.roles"
                :key="r"
                class="role-tag"
                size="small"
            >{{ r }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- ——— 两列内容卡片：个人资料 + 第三方绑定 ——— -->
    <el-row :gutter="20">

      <!-- 左列：个人资料编辑 -->
      <el-col :md="12" :xs="24">
        <div class="content-card">
          <div class="card-header">
            <div class="card-icon" style="background: linear-gradient(135deg, #11998e, #38ef7d);">
              <el-icon :size="16">
                <Edit/>
              </el-icon>
            </div>
            <div>
              <div class="card-title">个人资料</div>
              <div class="card-desc">修改昵称、邮箱和手机号</div>
            </div>
          </div>

          <el-form :model="form" class="profile-form" label-position="top">
            <!-- 用户名：只读，不可修改 -->
            <el-form-item label="用户名">
              <el-input
                  :model-value="userStore.userInfo?.username"
                  class="disabled-input"
                  disabled
              >
                <template #suffix>
                  <el-icon style="color: #c0c4cc;">
                    <Lock/>
                  </el-icon>
                </template>
              </el-input>
              <div class="field-tip">用户名不可修改</div>
            </el-form-item>

            <!-- 昵称 -->
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称"/>
            </el-form-item>

            <!-- 邮箱 -->
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱地址" type="email"/>
            </el-form-item>

            <!-- 手机号 -->
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号"/>
            </el-form-item>

            <!-- 保存按钮 -->
            <el-form-item>
              <el-button
                  :loading="updating"
                  class="save-btn"
                  type="primary"
                  @click="handleSave"
              >
                {{ updating ? '保存中...' : '保存修改' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <!-- 右列：第三方账号绑定 + 密码管理 -->
      <el-col :md="12" :xs="24">
        <!-- 密码管理卡片 -->
        <div class="content-card">
          <div class="card-header">
            <div class="card-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c);">
              <el-icon :size="16">
                <Lock/>
              </el-icon>
            </div>
            <div>
              <div class="card-title">密码安全</div>
              <div class="card-desc">{{ userStore.userInfo?.hasPassword ? '修改登录密码' : '设置登录密码，以便解绑第三方账号' }}</div>
            </div>
          </div>

          <el-form :model="pwdForm" class="profile-form" label-position="top">
            <!-- 已有密码时才显示旧密码字段 -->
            <el-form-item v-if="userStore.userInfo?.hasPassword" label="旧密码">
              <el-input v-model="pwdForm.oldPassword" placeholder="请输入旧密码" show-password type="password"/>
            </el-form-item>

            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" placeholder="请输入新密码（至少6位）" show-password type="password"/>
            </el-form-item>

            <el-form-item label="确认新密码">
              <el-input v-model="pwdForm.confirmPassword" placeholder="请再次输入新密码" show-password type="password"/>
            </el-form-item>

            <el-form-item>
              <el-button
                  :loading="savingPwd"
                  class="save-btn"
                  style="background: linear-gradient(135deg, #f093fb, #f5576c); border: none;"
                  type="primary"
                  @click="handleSetPassword"
              >
                {{ savingPwd ? '保存中...' : (userStore.userInfo?.hasPassword ? '修改密码' : '设置密码') }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第三方账号绑定卡片 -->
        <div class="content-card">
          <div class="card-header">
            <div class="card-icon" style="background: linear-gradient(135deg, #667eea, #764ba2);">
              <el-icon :size="16">
                <Connection/>
              </el-icon>
            </div>
            <div>
              <div class="card-title">第三方账号绑定</div>
              <div class="card-desc">绑定 OAuth2 认证平台账号，实现一键登录</div>
            </div>
          </div>

          <!-- 已绑定账号列表 -->
          <div v-if="oauthBindings.length" class="binding-list">
            <div
                v-for="b in oauthBindings"
                :key="b.provider"
                class="binding-item"
            >
              <!-- 提供商信息 -->
              <div class="binding-info">
                <div class="binding-dot"></div>
                <div>
                  <div class="binding-provider">{{ b.provider }}</div>
                  <div class="binding-uname">{{ b.oauthUsername }}</div>
                </div>
              </div>
              <!-- 解绑按钮 -->
              <el-button
                  :loading="unbinding === b.provider"
                  plain
                  size="small"
                  type="danger"
                  @click="handleUnbind(b.provider)"
              >解绑
              </el-button>
            </div>
          </div>

          <!-- 未绑定空状态 -->
          <div v-else class="binding-empty">
            <div class="binding-empty-icon">
              <el-icon :size="32">
                <Connection/>
              </el-icon>
            </div>
            <p>暂未绑定任何第三方账号</p>
          </div>

          <!-- 绑定按钮区域 -->
          <div class="binding-action">
            <el-button
                :disabled="isBound('auth-platform')"
                class="bind-btn"
                type="primary"
                @click="handleBind"
            >
              <el-icon>
                <Link/>
              </el-icon>
              绑定认证平台账号
            </el-button>
            <span v-if="isBound('auth-platform')" class="bound-tip">
              <el-icon style="vertical-align: -2px;"><CircleCheckFilled/></el-icon>
              已绑定
            </span>
          </div>

          <!-- 说明提示 -->
          <el-alert
              :closable="false"
              show-icon
              style="margin-top: 16px;"
              type="info"
          >
            <template #title>
              绑定后可使用认证平台账号一键登录，同一认证平台账号只能绑定一个本地账号。
            </template>
          </el-alert>
        </div>
      </el-col>

    </el-row>
  </div>
</template>

<script setup>
/**
 * 个人中心页 - 脚本逻辑
 *
 * 个人资料编辑：
 *  - onMounted 时读取 userInfo 填充表单（fetchUserInfo 刷新最新数据）
 *  - handleSave：PATCH /api/profile → 刷新 userInfo
 *
 * OAuth2 绑定管理：
 *  - handleBind：跳转到认证平台授权页（带 oauth_action=bind 标记）
 *  - handleUnbind：DELETE /api/oauth/unbind/{provider} → 刷新 userInfo
 *  - OAuthCallback.vue 检测到 oauth_action=bind 后，调用 bindOAuth 而非 oauthCallback
 */
import {ref, reactive, computed, onMounted} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useUserStore} from '../../store/user'
import {updateProfile, unbindOAuth, setPassword} from '../../api/user'
import {getOAuthUrl} from '../../api/auth'

const userStore = useUserStore()

/** 表单数据：双向绑定到可编辑字段 */
const form = reactive({nickname: '', email: '', phone: ''})

/** 保存中加载状态 */
const updating = ref(false)

/** 正在解绑的提供商名称（'' 表示无解绑操作进行中） */
const unbinding = ref('')

/** 密码表单 */
const pwdForm = reactive({oldPassword: '', newPassword: '', confirmPassword: ''})

/** 密码保存中加载状态 */
const savingPwd = ref(false)

/**
 * 当前用户已绑定的 OAuth 账号列表（从 userInfo 派生）
 * 结构：[{ provider: 'auth-platform', oauthUsername: '...' }]
 */
const oauthBindings = computed(() => userStore.userInfo?.oauthBindings || [])

/**
 * 判断指定提供商是否已绑定
 * @param {string} provider 提供商名称（如 'auth-platform'）
 */
const isBound = (provider) => oauthBindings.value.some(b => b.provider === provider)

/** 挂载时拉取最新用户信息并填充表单 */
onMounted(async () => {
  await userStore.fetchUserInfo()
  form.nickname = userStore.userInfo?.nickname || ''
  form.email = userStore.userInfo?.email || ''
  form.phone = userStore.userInfo?.phone || ''
})

/**
 * 保存个人资料
 * 调用 PUT /api/profile，后端合并更新，然后刷新 userInfo
 */
const handleSave = async () => {
  updating.value = true
  try {
    await updateProfile(form)
    await userStore.fetchUserInfo()
    ElMessage.success('个人资料保存成功')
  } catch (e) {
    // 错误已由 request.js 拦截器统一处理
  } finally {
    updating.value = false
  }
}

/**
 * 设置或修改密码
 */
const handleSetPassword = async () => {
  if (!pwdForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('密码不能少于6位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  const isFirstTime = !userStore.userInfo?.hasPassword
  savingPwd.value = true
  try {
    await setPassword({
      oldPassword: pwdForm.oldPassword || undefined,
      newPassword: pwdForm.newPassword,
    })
    await userStore.fetchUserInfo()
    ElMessage.success(isFirstTime ? '密码设置成功' : '密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    // 错误已由 request.js 拦截器统一处理
  } finally {
    savingPwd.value = false
  }
}

/**
 * 绑定认证平台 OAuth2 账号
 * 1. 生成 state 并存入 sessionStorage（CSRF 防护）
 * 2. 设置 oauth_action=bind，通知 OAuthCallback.vue 执行绑定而非登录
 * 3. 跳转到认证平台授权页
 */
const handleBind = async () => {
  try {
    const state = Math.random().toString(36).substring(7)
    sessionStorage.setItem('oauth_state', state)
    sessionStorage.setItem('oauth_action', 'bind') // OAuthCallback.vue 根据此值选择绑定或登录逻辑
    const res = await getOAuthUrl(state)
    window.location.href = res.data.authorizeUrl
  } catch (e) {
    ElMessage.error('获取授权地址失败')
  }
}

/**
 * 解绑 OAuth2 账号
 * 1. 弹出二次确认框防止误操作
 * 2. 调用 DELETE /api/oauth/unbind/{provider}
 * 3. 刷新 userInfo 更新绑定状态
 */
const handleUnbind = async (provider) => {
  try {
    await ElMessageBox.confirm(
        `确认解绑「${provider}」账号吗？解绑后将无法通过该平台登录本账号。`,
        '解绑确认',
        {type: 'warning', confirmButtonText: '确认解绑', cancelButtonText: '取消'}
    )
    unbinding.value = provider
    await unbindOAuth(provider)
    await userStore.fetchUserInfo()
    ElMessage.success('解绑成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('解绑失败，请稍后重试')
  } finally {
    unbinding.value = ''
  }
}
</script>

<style scoped>
/* ===================================================================
   页面容器
   =================================================================== */
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ===================================================================
   个人横幅：青绿色渐变，圆角
   =================================================================== */
.profile-banner {
  position: relative;
  background: linear-gradient(135deg, #0f7b6c 0%, #11998e 45%, #38ef7d 100%);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  color: #fff;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(17, 153, 142, 0.3);
}

/* 装饰光圈 */
.banner-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  pointer-events: none;
}

.banner-orb--1 {
  width: 220px;
  height: 220px;
  top: -70px;
  right: 80px;
}

.banner-orb--2 {
  width: 150px;
  height: 150px;
  bottom: -50px;
  right: 20px;
}

/* 横幅内层：头像 + 信息横向排列 */
.banner-inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 大头像：半透明白色背景 */
.banner-avatar {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.banner-name {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px;
}

.banner-meta {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 角色标签：半透明白色背景 */
.banner-roles {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.role-tag {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
  color: #fff !important;
}

/* ===================================================================
   内容卡片通用样式
   =================================================================== */
.content-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  padding: 20px;
  margin-bottom: 20px;
}

/* 卡片头部：图标 + 标题 + 副标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f5f5f5;
}

/* 图标圆角容器 */
.card-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* ===================================================================
   个人资料表单
   =================================================================== */
.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
  font-size: 13px;
}

/* 只读输入框灰色背景 */
.disabled-input :deep(.el-input__inner) {
  background: #f5f7fa;
  color: #909399;
}

/* 字段说明提示 */
.field-tip {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}

/* 保存按钮全宽，青绿渐变 */
.save-btn {
  width: 100%;
  background: linear-gradient(135deg, #11998e, #38ef7d);
  border: none;
  font-size: 14px;
}

.save-btn:hover {
  opacity: 0.9;
}

/* ===================================================================
   OAuth2 绑定管理
   =================================================================== */
.binding-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

/* 每个绑定账号行 */
.binding-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 8px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
}

.binding-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 绑定状态标记点 */
.binding-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #11998e;
  flex-shrink: 0;
}

.binding-provider {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.binding-uname {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 未绑定空状态 */
.binding-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0 16px;
  gap: 8px;
  margin-bottom: 16px;
}

.binding-empty-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.binding-empty p {
  font-size: 13px;
  color: #c0c4cc;
  margin: 0;
}

/* 绑定按钮操作区 */
.binding-action {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.bind-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}

.bind-btn:hover {
  opacity: 0.9;
}

/* 已绑定文字提示 */
.bound-tip {
  font-size: 13px;
  color: #67C23A;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>

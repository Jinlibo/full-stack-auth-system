<template>
  <!-- ============================================================
       个人中心页面
       功能：查看并编辑个人基础信息、修改登录密码
       ============================================================ -->
  <div class="page-container">

    <!-- ——— 页面头部 ——— -->
    <div class="page-header">
      <div class="page-header-icon">
        <el-icon :size="22">
          <UserFilled/>
        </el-icon>
      </div>
      <div class="page-header-text">
        <h2 class="page-title">个人中心</h2>
        <p class="page-desc">管理您的个人信息和账号安全设置</p>
      </div>
    </div>

    <!-- ——— 顶部用户信息展示卡片 ——— -->
    <div class="profile-banner">
      <!-- 大头像：显示昵称首字母 -->
      <div class="banner-avatar">
        <el-avatar :size="72" class="big-avatar">
          {{
            userStore.userInfo?.nickname?.charAt(0)?.toUpperCase()
            || userStore.userInfo?.username?.charAt(0)?.toUpperCase()
          }}
        </el-avatar>
      </div>
      <!-- 用户基本信息展示 -->
      <div class="banner-info">
        <div class="banner-name">
          {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
        </div>
        <div class="banner-meta">
          <!-- 用户名 -->
          <span class="meta-item">
            <el-icon><User/></el-icon>
            {{ userStore.userInfo?.username }}
          </span>
          <!-- 邮箱（若有） -->
          <span v-if="userStore.userInfo?.email" class="meta-item">
            <el-icon><Message/></el-icon>
            {{ userStore.userInfo?.email }}
          </span>
        </div>
        <!-- 角色标签列表 -->
        <div class="banner-roles">
          <el-tag
              v-for="r in userStore.userInfo?.roles"
              :key="r"
              class="role-tag"
              effect="plain"
              size="small"
          >{{ r }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- ——— 下方两列卡片：基础信息 + 修改密码 ——— -->
    <el-row :gutter="20">

      <!-- 左列：基础信息编辑表单 -->
      <el-col :md="12" :xs="24">
        <div class="content-card">
          <!-- 卡片头部 -->
          <div class="card-header">
            <el-icon class="card-header-icon">
              <Edit/>
            </el-icon>
            <span>基础信息</span>
          </div>
          <!-- 基础信息表单 -->
          <div class="card-body">
            <el-form :model="form" class="profile-form" label-width="80px">

              <!-- 用户名：禁止编辑，仅展示 -->
              <el-form-item label="用户名">
                <el-input
                    :model-value="userStore.userInfo?.username"
                    disabled
                    placeholder="用户名不可修改"
                />
              </el-form-item>

              <!-- 昵称 -->
              <el-form-item label="昵称">
                <el-input v-model="form.nickname" placeholder="请输入昵称"/>
              </el-form-item>

              <!-- 邮箱 -->
              <el-form-item label="邮箱">
                <el-input v-model="form.email" placeholder="请输入邮箱"/>
              </el-form-item>

              <!-- 手机号 -->
              <el-form-item label="手机号">
                <el-input v-model="form.phone" placeholder="请输入手机号"/>
              </el-form-item>

              <!-- 保存按钮 -->
              <el-form-item>
                <el-button :loading="savingInfo" type="primary" @click="handleSave">
                  <el-icon>
                    <Check/>
                  </el-icon>
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-col>

      <!-- 右列：修改密码表单 -->
      <el-col :md="12" :xs="24">
        <div class="content-card">
          <!-- 卡片头部 -->
          <div class="card-header">
            <el-icon class="card-header-icon card-header-icon--danger">
              <Lock/>
            </el-icon>
            <span>账号安全</span>
          </div>
          <!-- 修改密码表单 -->
          <div class="card-body">
            <el-form :model="pwdForm" class="profile-form" label-width="80px">

              <!-- 旧密码 -->
              <el-form-item label="旧密码">
                <el-input
                    v-model="pwdForm.oldPassword"
                    placeholder="请输入当前密码"
                    show-password
                    type="password"
                />
              </el-form-item>

              <!-- 新密码 -->
              <el-form-item label="新密码">
                <el-input
                    v-model="pwdForm.newPassword"
                    placeholder="请输入新密码（至少6位）"
                    show-password
                    type="password"
                />
              </el-form-item>

              <!-- 修改密码按钮 -->
              <el-form-item>
                <el-button :loading="savingPwd" type="danger" @click="handleChangePwd">
                  <el-icon>
                    <Lock/>
                  </el-icon>
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>

            <!-- 密码安全提示 -->
            <el-alert :closable="false" class="pwd-tip" show-icon type="warning">
              <template #default>
                建议使用包含大小写字母、数字和特殊字符的强密码，定期更换以保护账号安全
              </template>
            </el-alert>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 个人中心页面 - 脚本逻辑
 *
 * 功能说明：
 *  1. 从 userStore 读取当前登录用户信息并展示
 *  2. 支持修改昵称、邮箱、手机号，保存后刷新 store 中的用户信息
 *  3. 支持修改登录密码（需要输入旧密码验证身份）
 */
import {reactive, ref, onMounted} from 'vue'
import {useUserStore} from '../../store/user'
import {updateProfile, changePassword} from '../../api/user'
import {ElMessage} from 'element-plus'

/** 用户状态管理仓库，用于读取当前用户信息和刷新缓存 */
const userStore = useUserStore()

/**
 * 基础信息表单数据
 * - nickname:  昵称
 * - email:     邮箱
 * - phone:     手机号
 */
const form = reactive({nickname: '', email: '', phone: ''})

/**
 * 修改密码表单数据
 * - oldPassword:  当前密码（用于后端验证身份）
 * - newPassword:  新密码
 */
const pwdForm = reactive({oldPassword: '', newPassword: ''})

/** 保存基础信息时的加载状态 */
const savingInfo = ref(false)

/** 修改密码时的加载状态 */
const savingPwd = ref(false)

/** 组件挂载后将 store 中的用户信息回填到表单 */
onMounted(() => {
  if (userStore.userInfo) {
    // 用空字符串兜底，避免出现 undefined
    form.nickname = userStore.userInfo.nickname || ''
    form.email = userStore.userInfo.email || ''
    form.phone = userStore.userInfo.phone || ''
  }
})

/**
 * 保存基础信息
 * 调用 PUT /api/users/me/profile 接口更新用户信息
 * 成功后重新从服务器拉取用户信息以刷新 store 和页面顶部展示
 */
const handleSave = async () => {
  savingInfo.value = true
  try {
    await updateProfile(form)
    ElMessage.success('个人信息已保存')
    // 刷新 store 中的用户信息，确保页面头部头像、昵称等实时更新
    await userStore.fetchUserInfo()
  } finally {
    savingInfo.value = false
  }
}

/**
 * 修改登录密码
 * 调用 PUT /api/users/me/password 接口
 * 成功后清空密码输入框
 */
const handleChangePwd = async () => {
  // 前端简单校验：两个字段都必须填写
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写旧密码和新密码')
    return
  }
  // 简单长度校验
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码不能少于 6 位')
    return
  }
  savingPwd.value = true
  try {
    await changePassword(pwdForm)
    ElMessage.success('密码修改成功，请妥善保管新密码')
    // 清空密码输入框，避免敏感信息残留
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped>
/* ===================================================================
   页面容器
   =================================================================== */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===================================================================
   页面头部
   =================================================================== */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* ===================================================================
   顶部用户信息展示横幅
   =================================================================== */
.profile-banner {
  display: flex;
  align-items: center;
  gap: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 28px 32px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
}

/* 大头像 */
.big-avatar {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* 用户名显示名称（昵称 or 用户名） */
.banner-name {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}

/* 邮箱、用户名等元数据行 */
.banner-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 10px;
}

/* 单个元数据项：图标 + 文字 */
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  opacity: 0.85;
}

/* 角色标签行 */
.banner-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* 角色标签：半透明白色背景 */
.role-tag {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
  color: #fff !important;
  border-radius: 20px;
}

/* ===================================================================
   内容卡片
   =================================================================== */
.content-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef0f6;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  height: 100%;
}

/* 卡片头部：图标 + 标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
  background: #fafafa;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

/* 卡片头部图标：默认蓝色 */
.card-header-icon {
  color: #667eea;
}

/* 账号安全卡片头部图标：红色 */
.card-header-icon--danger {
  color: #f56c6c;
}

/* 卡片内容区 */
.card-body {
  padding: 24px 20px;
}

/* 个人信息/密码修改表单 */
.profile-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

/* 密码安全提示 */
.pwd-tip {
  margin-top: 4px;
  border-radius: 8px;
}
</style>

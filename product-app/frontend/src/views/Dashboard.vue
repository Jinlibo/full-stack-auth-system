<template>
  <!-- ============================================================
       仪表盘（Dashboard）
       功能：展示欢迎横幅、当前用户详情、OAuth2 绑定状态、快捷入口
       ============================================================ -->
  <div class="dashboard">

    <!-- ——— 欢迎横幅 ——— -->
    <div class="welcome-banner">
      <!-- 装饰光圈：CSS 绘制，增加视觉层次 -->
      <div class="banner-orb banner-orb--1"></div>
      <div class="banner-orb banner-orb--2"></div>

      <div class="banner-content">
        <div class="welcome-text">
          <h2>你好，{{ userStore.userInfo?.nickname || userStore.userInfo?.username }} 👋</h2>
          <p>欢迎回到产品应用平台，今天是 {{ today }}</p>
        </div>
        <!-- 头像：显示昵称首字母 -->
        <el-avatar :size="64" class="banner-avatar">
          {{ (userStore.userInfo?.nickname || userStore.userInfo?.username)?.charAt(0)?.toUpperCase() }}
        </el-avatar>
      </div>
    </div>

    <!-- ——— 信息行：用户资料 + OAuth2 绑定状态 ——— -->
    <el-row :gutter="20" style="margin-bottom: 20px;">

      <!-- 当前用户基础信息 -->
      <el-col :md="12" :xs="24">
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header-row">
              <el-icon>
                <UserFilled/>
              </el-icon>
              账号信息
            </div>
          </template>
          <el-descriptions :border="false" :column="1" class="info-desc">
            <el-descriptions-item label="用户名">
              <el-tag>{{ userStore.userInfo?.username }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="昵称">
              {{ userStore.userInfo?.nickname || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ userStore.userInfo?.email || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机">
              {{ userStore.userInfo?.phone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-space wrap>
                <el-tag
                    v-for="r in userStore.userInfo?.roles"
                    :key="r"
                    effect="plain"
                    size="small"
                    type="success"
                >{{ r }}
                </el-tag>
                <span
                    v-if="!userStore.userInfo?.roles?.length"
                    style="color: #c0c4cc;"
                >暂无角色</span>
              </el-space>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- OAuth2 绑定状态 -->
      <el-col :md="12" :xs="24">
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header-row">
              <el-icon>
                <Connection/>
              </el-icon>
              第三方账号绑定
            </div>
          </template>

          <!-- 已绑定的 OAuth 账号列表 -->
          <div
              v-if="userStore.userInfo?.oauthBindings?.length"
              class="oauth-list"
          >
            <div
                v-for="b in userStore.userInfo.oauthBindings"
                :key="b.provider"
                class="oauth-item"
            >
              <!-- 提供商名称标签 -->
              <div class="oauth-item-left">
                <div class="oauth-provider-dot"></div>
                <div>
                  <div class="oauth-provider-name">{{ b.provider }}</div>
                  <div class="oauth-username">{{ b.oauthUsername }}</div>
                </div>
              </div>
              <!-- 已绑定标记 -->
              <el-tag effect="plain" size="small" type="success">已绑定</el-tag>
            </div>
          </div>

          <!-- 未绑定提示 -->
          <div v-else class="oauth-empty">
            <el-icon :size="36" style="color: #dcdfe6;">
              <Link/>
            </el-icon>
            <p>暂未绑定第三方账号</p>
            <el-button
                plain
                size="small"
                type="primary"
                @click="$router.push('/profile')"
            >前往绑定
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ——— 快捷入口 ——— -->
    <el-card class="info-card" shadow="never">
      <template #header>
        <div class="card-header-row">
          <el-icon>
            <Grid/>
          </el-icon>
          快捷入口
        </div>
      </template>
      <div class="quick-links">
        <div
            v-for="link in quickLinks"
            :key="link.path"
            class="quick-link-item"
            @click="$router.push(link.path)"
        >
          <div :style="`background: ${link.bg}`" class="quick-link-icon">
            <el-icon :size="20">
              <component :is="link.icon"/>
            </el-icon>
          </div>
          <span>{{ link.label }}</span>
        </div>
      </div>
    </el-card>

  </div>
</template>

<script setup>
/**
 * 仪表盘页面 - 脚本逻辑
 *
 * - 从 userStore 读取当前登录用户信息（已在路由守卫中加载）
 * - today：当前日期的中文格式字符串（如：2026年3月14日星期六）
 * - quickLinks：静态快捷入口配置，点击跳转对应路由
 */
import {useUserStore} from '../store/user'

const userStore = useUserStore()

/**
 * 当前日期（中文本地化格式）
 * 格式：YYYY年M月D日 星期X
 */
const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
})

/**
 * 快捷入口配置
 * - label：显示名称
 * - path：点击后跳转的路由路径
 * - icon：Element Plus 图标组件名
 * - bg：图标区半透明背景色
 */
const quickLinks = [
  {label: '用户管理', path: '/users', icon: 'User', bg: 'rgba(17,153,142,0.12)'},
  {label: '个人中心', path: '/profile', icon: 'UserFilled', bg: 'rgba(56,239,125,0.12)'},
]
</script>

<style scoped>
/* ===================================================================
   仪表盘主容器
   =================================================================== */
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ===================================================================
   欢迎横幅：青绿色渐变，圆角卡片
   =================================================================== */
.welcome-banner {
  position: relative;
  background: linear-gradient(135deg, #0f7b6c 0%, #11998e 45%, #38ef7d 100%);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  color: #fff;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(17, 153, 142, 0.3);
}

/* 装饰性光圈：绝对定位，增加视觉层次 */
.banner-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.banner-orb--1 {
  width: 200px;
  height: 200px;
  top: -60px;
  right: 120px;
}

.banner-orb--2 {
  width: 140px;
  height: 140px;
  bottom: -40px;
  right: 40px;
}

/* 横幅内容：左右布局（文字 + 头像） */
.banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome-text h2 {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px;
}

.welcome-text p {
  font-size: 14px;
  opacity: 0.85;
  margin: 0;
}

/* 头像：半透明白色背景 */
.banner-avatar {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-weight: 700;
  font-size: 26px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

/* ===================================================================
   信息卡片通用样式
   =================================================================== */
.info-card {
  border-radius: 12px !important;
  border: 1px solid #f0f0f0 !important;
  margin-bottom: 20px;
}

/* 卡片头部：图标 + 标题 */
.info-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f5f5f5;
}

.card-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

/* el-descriptions label 列 */
.info-desc :deep(.el-descriptions__label) {
  width: 70px;
  color: #909399;
  font-size: 13px;
}

.info-desc :deep(.el-descriptions__content) {
  font-size: 13px;
  color: #303133;
}

/* ===================================================================
   OAuth2 绑定状态
   =================================================================== */
.oauth-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.oauth-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
}

.oauth-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 提供商颜色标记点：青绿色 */
.oauth-provider-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #11998e;
  flex-shrink: 0;
}

.oauth-provider-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.oauth-username {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 未绑定空状态 */
.oauth-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0 16px;
  gap: 8px;
}

.oauth-empty p {
  font-size: 13px;
  color: #c0c4cc;
  margin: 0 0 4px;
}

/* ===================================================================
   快捷入口网格
   =================================================================== */
.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  padding: 4px 0;
}

.quick-link-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.18s;
  font-size: 14px;
  color: #303133;
}

.quick-link-item:hover {
  background: #edfaf7;
  border-color: #b2e5da;
  color: #11998e;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(17, 153, 142, 0.1);
}

/* 快捷入口图标容器 */
.quick-link-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #11998e;
  flex-shrink: 0;
}
</style>

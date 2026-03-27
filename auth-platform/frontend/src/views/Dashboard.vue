<template>
  <div class="premium-dashboard">
    <!-- Welcome Banner with Animated Mesh/Particles -->
    <div class="premium-banner">
      <div class="banner-bg-elements">
        <div class="bg-shape shape-1"></div>
        <div class="bg-shape shape-2"></div>
      </div>
      <div class="banner-content">
        <div class="welcome-text">
          <h2>你好，{{ userStore.userInfo?.nickname || userStore.userInfo?.username }} 👋</h2>
          <p>欢迎回到系统管理中枢，今天是 {{ today }}</p>
        </div>
        <div class="welcome-avatar-container">
          <div class="avatar-ring"></div>
          <el-avatar :size="68" class="premium-avatar">
            {{ (userStore.userInfo?.nickname || userStore.userInfo?.username)?.charAt(0)?.toUpperCase() }}
          </el-avatar>
        </div>
      </div>
    </div>

    <el-row :gutter="24" class="stats-row">
      <el-col v-for="stat in stats" :key="stat.label" :sm="6" :xs="12">
        <div class="premium-stat-card group">
          <div class="stat-icon-wrap" :style="{ '--accent': stat.color, '--accent-bg': stat.bg }">
            <el-icon :size="26"><component :is="stat.icon"/></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stat.value ?? '--' }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
          <!-- Decorative line on hover -->
          <div class="hover-line" :style="{ background: stat.color }"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24">
      <el-col :md="12" :xs="24">
        <el-card class="premium-info-card" shadow="never">
          <template #header>
            <div class="card-header-styled">
              <div class="header-icon primary-bg"><el-icon><UserFilled/></el-icon></div>
              <span>当前驻留凭证</span>
            </div>
          </template>
          <el-descriptions :border="false" :column="1" class="info-desc">
            <el-descriptions-item label="网络标识">
              <el-tag effect="light" round type="info" class="premium-tag">{{ userStore.userInfo?.username }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="身份昵称">
              <span class="info-text">{{ userStore.userInfo?.nickname || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="联络邮箱">
              <span class="info-text">{{ userStore.userInfo?.email || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="当前权限">
              <el-space wrap>
                <el-tag
                    v-for="r in userStore.userInfo?.roles"
                    :key="r"
                    effect="dark"
                    size="small"
                    color="#18181b"
                    class="role-tag"
                >{{ r }}
                </el-tag>
                <span v-if="!userStore.userInfo?.roles?.length" class="empty-text">无特权</span>
              </el-space>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :md="12" :xs="24">
        <el-card class="premium-info-card" shadow="never">
          <template #header>
            <div class="card-header-styled">
              <div class="header-icon secondary-bg"><el-icon><DataAnalysis/></el-icon></div>
              <span>核心调度台</span>
            </div>
          </template>
          <div class="quick-links-grid">
            <div
                v-for="link in quickLinks"
                :key="link.path"
                class="premium-link-card"
                @click="$router.push(link.path)"
                :style="{'--hover-color': link.color}"
            >
              <div class="link-icon" :style="{ background: link.bg, color: link.color }">
                <el-icon :size="22"><component :is="link.icon"/></el-icon>
              </div>
              <span class="link-label">{{ link.label }}</span>
              <el-icon class="link-arrow"><ArrowRight/></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {useUserStore} from '../store/user'

const userStore = useUserStore()

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
})

const stats = ref([
  {label: '活跃物理节点', value: null, icon: 'User', color: '#3b82f6', bg: 'rgba(59, 130, 246, 0.1)'},
  {label: '策略分配角色', value: null, icon: 'UserFilled', color: '#8b5cf6', bg: 'rgba(139, 92, 246, 0.1)'},
  {label: '边缘应用实例', value: null, icon: 'Box', color: '#ec4899', bg: 'rgba(236, 72, 153, 0.1)'},
  {label: '细粒度访问链', value: null, icon: 'Key', color: '#10b981', bg: 'rgba(16, 185, 129, 0.1)'},
])

const quickLinks = [
  {label: '全局用户身份管控', path: '/system/user', icon: 'User', color: '#3b82f6', bg: 'rgba(59,130,246,0.1)'},
  {label: '企业级基础权限配置', path: '/system/role', icon: 'UserFilled', color: '#8b5cf6', bg: 'rgba(139,92,246,0.1)'},
  {label: 'OAuth 客户端授权台', path: '/product/list', icon: 'Grid', color: '#ec4899', bg: 'rgba(236,72,153,0.1)'},
  {label: '元策略访问权限表', path: '/system/permission', icon: 'Key', color: '#10b981', bg: 'rgba(16,185,129,0.1)'},
]
</script>

<style scoped>
.premium-dashboard {
  display: flex;
  flex-direction: column;
  gap: 8px; 
  font-family: 'Inter', system-ui, sans-serif;
  color: #18181b;
}

/* --- Banner --- */
.premium-banner {
  position: relative;
  background: #09090b; 
  border-radius: 20px;
  padding: 36px 40px;
  margin-bottom: 28px;
  color: #fff;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(9, 9, 11, 0.15);
}

.banner-bg-elements {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.bg-shape {
  position: absolute;
  filter: blur(60px);
  border-radius: 50%;
  opacity: 0.35;
}

.shape-1 { width: 400px; height: 400px; background: #3b82f6; top: -200px; right: -50px; }
.shape-2 { width: 300px; height: 300px; background: #8b5cf6; bottom: -120px; right: 250px; }

.banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome-text h2 {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 8px;
  letter-spacing: 0.5px;
}

.welcome-text p {
  font-size: 15px;
  color: #a1a1aa;
  margin: 0;
}

.welcome-avatar-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-ring {
  position: absolute;
  width: 78px;
  height: 78px;
  border-radius: 50%;
  border: 2px solid transparent;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: spin 6s linear infinite;
}

@keyframes spin { 100% { transform: rotate(360deg); } }

.premium-avatar {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-weight: 700;
  font-size: 28px;
  box-shadow: 0 8px 16px rgba(0,0,0,0.3);
  backdrop-filter: blur(10px);
}

/* --- Stats row --- */
.stats-row { margin-bottom: 24px; }

.premium-stat-card {
  position: relative;
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  border: 1px solid #f4f4f5;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  cursor: default;
  margin-bottom: 20px;
}

.premium-stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
  border-color: #e4e4e7;
}

.hover-line {
  position: absolute;
  bottom: 0; left: 0;
  height: 3px;
  width: 0;
  transition: width 0.3s ease;
}

.premium-stat-card:hover .hover-line { width: 100%; }

.stat-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--accent-bg);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.premium-stat-card:hover .stat-icon-wrap { transform: scale(1.1) rotate(5deg); }

.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: 800; color: #18181b; line-height: 1.2; }
.stat-label { font-size: 13px; color: #71717a; font-weight: 500; margin-top: 4px; }

/* --- Info Cards --- */
.premium-info-card {
  border-radius: 16px !important;
  border: 1px solid #f4f4f5 !important;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05) !important;
  transition: box-shadow 0.3s ease;
}

.premium-info-card:hover {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.08) !important;
}

.premium-info-card :deep(.el-card__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f4f4f5;
  background: #fafafa;
  border-radius: 16px 16px 0 0;
}

.card-header-styled {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #18181b;
}

.header-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.primary-bg { background: linear-gradient(135deg, #3b82f6, #60a5fa); }
.secondary-bg { background: linear-gradient(135deg, #8b5cf6, #a78bfa); }

.info-desc :deep(.el-descriptions__label) { width: 80px; color: #71717a; font-weight: 500; }
.info-text { font-weight: 500; color: #18181b; }
.role-tag { border: none; padding: 4px 12px; font-weight: 600; }
.empty-text { color: #a1a1aa; font-style: italic; font-size: 13px; }

.quick-links-grid { display: grid; grid-template-columns: 1fr; gap: 16px; padding: 8px 0; }

.premium-link-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e4e4e7;
  cursor: pointer;
  transition: all 0.2s ease;
}

.premium-link-card:hover {
  transform: translateX(6px);
  border-color: var(--hover-color);
  background: #fafafa;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.link-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.link-label { font-size: 15px; font-weight: 600; color: #27272a; flex: 1; }
.link-arrow { color: #a1a1aa; font-size: 18px; transition: transform 0.2s ease, color 0.2s ease; }
.premium-link-card:hover .link-arrow { transform: translateX(6px); color: var(--hover-color); }
</style>

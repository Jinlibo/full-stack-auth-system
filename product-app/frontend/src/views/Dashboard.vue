<template>
  <div class="premium-light-dashboard">
    <!-- Welcome Banner with beautiful fluid gradient -->
    <div class="premium-banner">
      <div class="banner-bg-elements">
        <div class="fluid-shape shape-mint"></div>
        <div class="fluid-shape shape-sky"></div>
      </div>
      <div class="banner-content">
        <div class="welcome-text">
          <h2>你好，{{ userStore.userInfo?.nickname || userStore.userInfo?.username }} 👋</h2>
          <p>欢迎回到应用服务平台，今天是 {{ today }}</p>
        </div>
        <div class="welcome-avatar-container">
          <div class="avatar-ring"></div>
          <el-avatar :size="68" class="premium-avatar">
            {{ (userStore.userInfo?.nickname || userStore.userInfo?.username)?.charAt(0)?.toUpperCase() }}
          </el-avatar>
        </div>
      </div>
    </div>

    <!-- Info Cards Row -->
    <el-row :gutter="24">
      <el-col :md="12" :xs="24">
        <el-card class="premium-info-card" shadow="never">
          <template #header>
            <div class="card-header-styled">
              <div class="header-icon teal-bg"><el-icon><UserFilled/></el-icon></div>
              <span>终端账号信息</span>
            </div>
          </template>
          <el-descriptions :border="false" :column="1" class="info-desc">
            <el-descriptions-item label="统一身份标识">
              <el-tag effect="light" round class="premium-tag">{{ userStore.userInfo?.username }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="对外显示昵称">
              <span class="info-text">{{ userStore.userInfo?.nickname || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="企业联系邮箱">
              <span class="info-text">{{ userStore.userInfo?.email || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="绑定手机号码">
              <span class="info-text">{{ userStore.userInfo?.phone || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="分配权限角色">
              <el-space wrap>
                <el-tag
                    v-for="r in userStore.userInfo?.roles"
                    :key="r"
                    effect="plain"
                    size="small"
                    color="#f0fdf4"
                    class="role-tag"
                >{{ r }}
                </el-tag>
                <span v-if="!userStore.userInfo?.roles?.length" class="empty-text">普通访问权限</span>
              </el-space>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- OAuth Bindings -->
      <el-col :md="12" :xs="24">
        <el-card class="premium-info-card" shadow="never">
          <template #header>
            <div class="card-header-styled">
              <div class="header-icon sky-bg"><el-icon><Connection/></el-icon></div>
              <span>鉴权中心身份解耦状态</span>
            </div>
          </template>

          <div v-if="userStore.userInfo?.oauthBindings?.length" class="oauth-list">
            <div v-for="b in userStore.userInfo.oauthBindings" :key="b.provider" class="premium-oauth-item">
              <div class="oauth-item-left">
                <div class="oauth-icon-box"><el-icon><Link/></el-icon></div>
                <div>
                  <div class="oauth-provider-name">{{ b.provider }} 通行证协议</div>
                  <div class="oauth-username">{{ b.oauthUsername }}</div>
                </div>
              </div>
              <div class="status-badge"><span class="dot"></span>已完成核心绑定</div>
            </div>
          </div>

          <div v-else class="oauth-empty">
            <div class="empty-shield"><el-icon :size="40"><WarnTriangleFilled/></el-icon></div>
            <h4>身份资产尚孤立</h4>
            <p>请链接全栈 Auth Platform，启用零接触式单点通行流。</p>
            <el-button class="action-btn-light" @click="$router.push('/profile')">马上建立互信</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Links -->
    <el-card class="premium-info-card mt-24" shadow="never">
      <template #header>
        <div class="card-header-styled">
          <div class="header-icon indigo-bg"><el-icon><Grid/></el-icon></div>
          <span>全局服务快捷调度台</span>
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
            <el-icon :size="24"><component :is="link.icon"/></el-icon>
          </div>
          <div class="link-text-wrap">
            <span class="link-label">{{ link.label }}</span>
            <span class="link-desc">{{ link.desc }}</span>
          </div>
          <el-icon class="link-arrow"><ArrowRight/></el-icon>
        </div>
      </div>
    </el-card>

  </div>
</template>

<script setup>
import {useUserStore} from '../store/user'

const userStore = useUserStore()

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
})

const quickLinks = [
  {label: '内部人员调配系统', desc: '检索系统内部资源并控制子用户全生命周期', path: '/users', icon: 'User', color: '#0ea5e9', bg: 'rgba(14, 165, 233, 0.1)'},
  {label: '个人专属安全中心', desc: '维护您的私密资料与认证参数设定', path: '/profile', icon: 'UserFilled', color: '#10b981', bg: 'rgba(16, 185, 129, 0.1)'},
]
</script>

<style scoped>
.premium-light-dashboard {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-family: 'Inter', system-ui, sans-serif;
  color: #0f172a;
}

/* --- Banner --- */
.premium-banner {
  position: relative;
  background: linear-gradient(135deg, #0f7b6c 0%, #11998e 45%, #14b8a6 100%);
  border-radius: 20px;
  padding: 36px 40px;
  margin-bottom: 28px;
  color: #fff;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(17, 153, 142, 0.25);
  border: 1px solid rgba(255,255,255,0.1);
}

.banner-bg-elements {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.fluid-shape {
  position: absolute;
  filter: blur(80px);
  border-radius: 50%;
  opacity: 0.8;
  mix-blend-mode: color-dodge;
  animation: floatBanner 20s infinite ease-in-out alternate;
}

.shape-mint { width: 350px; height: 350px; background: rgba(52, 211, 153, 0.6); top: -150px; right: -50px; }
.shape-sky { width: 300px; height: 300px; background: rgba(56, 189, 248, 0.5); bottom: -100px; right: 250px; animation-delay: -5s;}

@keyframes floatBanner {
  0% { transform: translate(0,0) rotate(0deg); }
  100% { transform: translate(-30px, 30px) scale(1.1) rotate(20deg); }
}

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
  text-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.welcome-text p {
  font-size: 15px;
  color: rgba(255,255,255,0.9);
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
  border: 2px solid rgba(255,255,255,0.4);
  background: transparent;
  animation: pulse-ring 2s infinite;
}

@keyframes pulse-ring {
  0% { transform: scale(0.9); opacity: 1; }
  100% { transform: scale(1.3); opacity: 0; }
}

.premium-avatar {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-weight: 700;
  font-size: 28px;
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255,255,255,0.8);
}

/* --- Info Cards --- */
.premium-info-card {
  border-radius: 20px !important;
  border: none !important;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.04), 0 1px 3px rgba(0,0,0,0.02) !important;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  margin-bottom: 24px;
}

.premium-info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.06), 0 4px 6px rgba(0,0,0,0.03) !important;
}

.premium-info-card :deep(.el-card__header) {
  padding: 24px 28px;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
  border-radius: 20px 20px 0 0;
}

.mt-24 { margin-top: 0; }

.card-header-styled {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
}

.header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
}
.teal-bg { background: linear-gradient(135deg, #14b8a6, #0d9488); }
.sky-bg { background: linear-gradient(135deg, #0ea5e9, #0284c7); }
.indigo-bg { background: linear-gradient(135deg, #6366f1, #4f46e5); }

.info-desc :deep(.el-descriptions__label) { width: 100px; color: #64748b; font-weight: 600; font-size: 14px;}
.info-text { font-weight: 600; color: #334155; font-size: 14px;}
.premium-tag { font-weight: 600; font-size: 13px; }
.role-tag { border: 1px solid #bbf7d0; padding: 4px 12px; font-weight: 700; color: #166534; border-radius: 8px;}
.empty-text { color: #94a3b8; font-style: italic; font-size: 13px; }

/* OAuth System */
.oauth-list { display: flex; flex-direction: column; gap: 14px; }

.premium-oauth-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  transition: all 0.2s ease;
}

.premium-oauth-item:hover {
  background: #f0fdf4;
  border-color: #bcf0da;
  transform: translateX(4px);
}

.oauth-item-left { display: flex; align-items: center; gap: 14px; }

.oauth-icon-box {
  width: 40px; height: 40px;
  background: #ffffff;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #10b981;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  font-size: 18px;
}

.oauth-provider-name { font-size: 15px; font-weight: 700; color: #1e293b; }
.oauth-username { font-size: 13px; color: #64748b; margin-top: 2px; }

.status-badge {
  display: inline-flex; align-items: center; gap: 6px;
  background: #dcfce7; color: #166534;
  padding: 6px 12px; border-radius: 20px;
  font-size: 12px; font-weight: 700;
}
.status-badge .dot { width: 6px; height: 6px; background: #22c55e; border-radius: 50%; animation: blink 2s infinite; }

@keyframes blink { 0%, 100% {opacity:1;} 50% {opacity:0.4;} }

.oauth-empty {
  display: flex; flex-direction: column; align-items: center;
  padding: 32px 0 20px; text-align: center;
}

.empty-shield {
  width: 72px; height: 72px;
  background: #fef2f2; border-radius: 50%;
  color: #ef4444; display: flex; align-items: center; justify-content: center;
  margin-bottom: 16px; border: 4px solid #fff; box-shadow: 0 10px 25px rgba(239, 68, 68, 0.15);
}

.oauth-empty h4 { font-size: 18px; font-weight: 800; color: #0f172a; margin: 0 0 8px; }
.oauth-empty p { font-size: 14px; color: #64748b; margin: 0 0 24px; max-width: 280px; line-height: 1.6;}

.action-btn-light {
  background: #f8fafc; border: 1px solid #cbd5e1;
  color: #0f172a; font-weight: 600;
  padding: 10px 24px; height: auto; border-radius: 10px;
  transition: all 0.2s;
}
.action-btn-light:hover { background: #0ea5e9; border-color: #0ea5e9; color: #fff; transform: translateY(-2px); box-shadow: 0 10px 20px rgba(14, 165, 233, 0.2); }

/* Quick Links System */
.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  padding: 8px 0;
}

.premium-link-card {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #f1f5f9;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 6px rgba(0,0,0,0.02);
}

.premium-link-card:hover {
  transform: translateY(-4px);
  border-color: var(--hover-color);
  background: #f8fafc;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.06);
}

.link-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 18px;
  flex-shrink: 0;
}

.link-text-wrap { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.link-label { font-size: 16px; font-weight: 800; color: #1e293b; }
.link-desc { font-size: 13px; color: #64748b; line-height: 1.4; }

.link-arrow {
  color: #cbd5e1;
  font-size: 20px;
  transition: transform 0.2s ease, color 0.2s ease;
}

.premium-link-card:hover .link-arrow {
  transform: translateX(6px);
  color: var(--hover-color);
}
</style>

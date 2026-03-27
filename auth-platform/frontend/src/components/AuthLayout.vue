<template>
  <div class="premium-login-bg">
    <!-- Animated Orbs -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>

    <div class="glass-container">
      <div class="glass-left">
        <div class="brand-showcase">
          <div class="logo-box">
            <el-icon :size="40"><Monitor /></el-icon>
          </div>
          <h1 class="brand-title">认证授权平台<span class="dot">.</span></h1>
          <p class="brand-slogan">
            基于先进架构构建的现代级<br />统一身份与权限管理防线
          </p>
          <div class="feature-badges">
            <span class="badge"><el-icon><Key/></el-icon> 企业级 RBAC</span>
            <span class="badge"><el-icon><Connection/></el-icon> OAuth 2.0</span>
            <span class="badge"><el-icon><Cpu/></el-icon> 分布式管控</span>
          </div>
        </div>
      </div>
      
      <div class="glass-right">
        <!-- 业务表单区块注入插槽或路由容器 -->
        <slot>
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style>
/* 局部路由切换动画，仅应用于右侧表单区 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.fade-slide-enter-from { opacity: 0; transform: translateX(15px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-15px); }
</style>

<style scoped>
.premium-login-bg {
  min-height: 100vh;
  background: #09090b; /* Very dark zinc */
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  mix-blend-mode: color-dodge;
  animation: float 20s infinite ease-in-out alternate;
  pointer-events: none;
}

.orb-1 { width: 50vw; height: 50vw; background: #3b82f6; top: -10vw; left: -10vw; opacity: 0.15; }
.orb-2 { width: 40vw; height: 40vw; background: #8b5cf6; bottom: -5vw; right: -5vw; opacity: 0.2; animation-delay: -5s; }
.orb-3 { width: 30vw; height: 30vw; background: #ec4899; top: 30%; left: 40%; opacity: 0.1; animation-duration: 25s; }

@keyframes float {
  0% { transform: translateY(0) scale(1) rotate(0deg); }
  100% { transform: translateY(-50px) scale(1.1) rotate(10deg); }
}

.glass-container {
  width: 1100px;
  max-width: 95vw;
  height: 650px;
  max-height: 95vh;
  display: flex;
  border-radius: 24px;
  background: rgba(24, 24, 27, 0.4); 
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.5), inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  overflow: hidden;
  z-index: 10;
}

.glass-left {
  flex: 5;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
}

.brand-showcase {
  position: relative;
  z-index: 2;
}

.logo-box {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(139, 92, 246, 0.3);
}

.brand-title {
  font-size: 38px;
  font-weight: 800;
  color: #ffffff;
  margin: 0 0 16px;
  letter-spacing: 1px;
}

.brand-title .dot {
  color: #8b5cf6;
}

.brand-slogan {
  font-size: 16px;
  line-height: 1.8;
  color: #a1a1aa;
  margin: 0 0 40px;
}

.feature-badges {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  color: #d4d4d8;
  font-size: 14px;
  font-weight: 500;
  width: fit-content;
  transition: all 0.3s ease;
}

.badge:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(139, 92, 246, 0.3);
  transform: translateX(5px);
  color: #fff;
}

.glass-right {
  flex: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

@media (max-width: 900px) {
  .glass-left { display: none; }
  .glass-container { height: auto; padding: 40px 0; border-radius: 16px; }
  .glass-right { padding: 40px 30px; }
}
</style>

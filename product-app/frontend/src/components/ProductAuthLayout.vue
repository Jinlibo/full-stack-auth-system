<template>
  <div class="premium-light-bg">
    <!-- Animated Light Orbs -->
    <div class="orb orb-mint"></div>
    <div class="orb orb-sky"></div>
    <div class="orb orb-rose"></div>

    <div class="glass-modern-container">
      <div class="split-left">
        <div class="brand-showcase">
          <div class="logo-box">
            <el-icon :size="46"><Box /></el-icon>
          </div>
          <h1 class="brand-title">微架构协同客户端</h1>
          <p class="brand-slogan">立足云端，引领未来的现代化分布式<br/>核心业务生态网关</p>
          <div class="feature-pills">
            <span class="pill"><el-icon><Monitor/></el-icon> 统一安全身份体系</span>
            <span class="pill"><el-icon><Connection/></el-icon> 标准 OAuth2 内核</span>
            <span class="pill"><el-icon><DataLine/></el-icon> 账号多端平滑无缝融合</span>
          </div>
        </div>
      </div>

      <div class="split-right">
        <!-- 业务内容插槽或路由过渡区 -->
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
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}
.fade-slide-enter-from { opacity: 0; transform: translateX(16px); }
.fade-slide-leave-to   { opacity: 0; transform: translateX(-16px); }
</style>

<style scoped>
.premium-light-bg {
  min-height: 100vh;
  background: #f8fafc; 
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
  filter: blur(100px);
  animation: float 25s infinite ease-in-out alternate;
  pointer-events: none;
  z-index: 0;
}

.orb-mint { width: 50vw; height: 50vw; background: rgba(52, 211, 153, 0.4); top: -10vw; left: -10vw; }
.orb-sky { width: 45vw; height: 45vw; background: rgba(56, 189, 248, 0.4); bottom: -10vw; right: -5vw; animation-delay: -5s; }
.orb-rose { width: 35vw; height: 35vw; background: rgba(251, 113, 133, 0.3); top: 20%; left: 35%; animation-duration: 20s; }

@keyframes float {
  0% { transform: translateY(0) scale(1); }
  100% { transform: translateY(-40px) scale(1.1); }
}

.glass-modern-container {
  width: 1000px;
  max-width: 95vw;
  height: 600px;
  max-height: 90vh;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-radius: 28px;
  box-shadow: 0 40px 100px -20px rgba(0, 0, 0, 0.1), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  display: flex;
  overflow: hidden;
  z-index: 10;
}

.split-left {
  flex: 5;
  background: linear-gradient(145deg, rgba(20, 184, 166, 0.95) 0%, rgba(14, 165, 233, 0.9) 100%);
  padding: 60px;
  display: flex;
  align-items: center;
  position: relative;
  color: #fff;
}

.split-left::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-image: radial-gradient(rgba(255,255,255,0.2) 1px, transparent 1px);
  background-size: 20px 20px;
  opacity: 0.3;
}

.brand-showcase { position: relative; z-index: 2; width: 100%; }

.logo-box {
  width: 72px; height: 72px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  margin-bottom: 30px;
}

.brand-title { font-size: 34px; font-weight: 800; margin: 0 0 16px; letter-spacing: 0.5px; }
.brand-slogan { font-size: 16px; opacity: 0.9; margin: 0 0 40px; line-height: 1.6; }

.feature-pills { display: flex; flex-direction: column; gap: 16px; }

.pill {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  backdrop-filter: blur(10px);
  transition: all 0.3s;
  width: fit-content;
}

.pill:hover { background: rgba(255, 255, 255, 0.25); transform: translateX(6px); }

.split-right {
  flex: 6;
  padding: 40px 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 900px) {
  .split-left { display: none; }
  .glass-modern-container { height: auto; margin: 20px; border-radius: 20px; }
  .split-right { padding: 40px 30px; }
}
</style>

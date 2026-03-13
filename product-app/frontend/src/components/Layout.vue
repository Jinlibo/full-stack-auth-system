<template>
  <!-- ============================================================
       全局布局框架（Layout）
       结构：左侧折叠侧边栏 + 右侧内容区（顶部 Header + 主内容）
       侧边栏支持折叠/展开，折叠时只显示图标，宽度缩小为 64px
       ============================================================ -->
  <el-container class="app-layout">

    <!-- ——— 左侧侧边栏 ——— -->
    <el-aside
        :width="collapsed ? '64px' : '220px'"
        class="sidebar"
    >
      <!-- Logo 区域：折叠时只显示图标，展开时显示文字品牌名 -->
      <div :class="{ 'sidebar-logo--collapsed': collapsed }" class="sidebar-logo">
        <div class="logo-icon">
          <el-icon :size="20">
            <Box/>
          </el-icon>
        </div>
        <!-- v-show 而非 v-if：保持 DOM，避免展开/折叠时文字闪烁 -->
        <span v-show="!collapsed" class="logo-text">产品应用平台</span>
      </div>

      <!-- 导航菜单
           router=true：点击 menu-item 自动调用 router.push(index)
           collapse：折叠状态下菜单只显示图标，自动隐藏文字
           collapse-transition=false：禁用折叠动画（避免与侧边栏宽度动画冲突）
      -->
      <el-menu
          :collapse="collapsed"
          :collapse-transition="false"
          :default-active="$route.path"
          class="sidebar-menu"
          router
      >
        <!-- 仪表盘 -->
        <el-menu-item class="menu-item" index="/dashboard">
          <el-icon>
            <DataAnalysis/>
          </el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>

        <!-- 用户管理 -->
        <el-menu-item class="menu-item" index="/users">
          <el-icon>
            <User/>
          </el-icon>
          <template #title>用户管理</template>
        </el-menu-item>

        <!-- 个人中心 -->
        <el-menu-item class="menu-item" index="/profile">
          <el-icon>
            <UserFilled/>
          </el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- ——— 右侧内容区 ——— -->
    <el-container class="main-container">

      <!-- 顶部 Header -->
      <el-header class="app-header">
        <!-- 左侧：折叠按钮 + 当前页面面包屑 -->
        <div class="header-left">
          <!-- 折叠/展开按钮，点击切换 collapsed 状态 -->
          <el-icon
              :size="20"
              class="collapse-btn"
              @click="collapsed = !collapsed"
          >
            <component :is="collapsed ? 'Expand' : 'Fold'"/>
          </el-icon>

          <!-- 面包屑：根据当前路由 meta.title 显示路径 -->
          <el-breadcrumb class="breadcrumb" separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="$route.meta?.title">
              {{ $route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 右侧：用户昵称 + 头像下拉菜单 -->
        <div class="header-right">
          <!-- 用户昵称（桌面端显示，移动端隐藏） -->
          <span class="username-label">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
          </span>

          <!-- 用户头像：点击展开下拉菜单 -->
          <el-dropdown placement="bottom-end" trigger="click">
            <!-- 头像：显示昵称/用户名首字母 -->
            <el-avatar class="user-avatar">
              {{ (userStore.userInfo?.nickname || userStore.userInfo?.username)?.charAt(0)?.toUpperCase() }}
            </el-avatar>

            <!-- 下拉菜单项 -->
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon>
                    <UserFilled/>
                  </el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon>
                    <SwitchButton/>
                  </el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区：渲染当前路由对应的页面组件 -->
      <el-main class="app-main">
        <router-view/>
      </el-main>
    </el-container>

  </el-container>
</template>

<script setup>
/**
 * 全局布局组件 - 脚本逻辑
 *
 * 功能：
 * - collapsed：控制侧边栏折叠状态，持久化到 localStorage（刷新后保持）
 * - handleLogout：调用后端注销接口（删除 Redis Token）+ 清空本地 Pinia 状态 + 跳转登录页
 *
 * 注意：
 * - el-menu 的 router 属性配合 index 值做路由跳转，index 必须与路由 path 完全一致
 * - 侧边栏宽度通过 :width 动态绑定实现折叠动画（CSS transition 在 .sidebar 中配置）
 */
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useUserStore} from '../store/user'
import {logout} from '../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

/**
 * 侧边栏折叠状态
 * 从 localStorage 读取持久化的折叠偏好，默认展开（false）
 */
const collapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true')

/**
 * 退出登录处理
 * 1. 调用后端 /api/auth/logout（将当前 Token 加入 Redis 黑名单）
 * 2. 清除本地 Pinia store 中的 token 和 userInfo
 * 3. 跳转到登录页
 * 注意：即使后端调用失败（如 Token 已过期），也应继续清除本地状态
 */
const handleLogout = async () => {
  try {
    await logout()
  } catch (e) {
    // 忽略后端注销失败（Token 可能已过期），继续清理本地状态
  }
  userStore.logout()
  router.push('/login')
  ElMessage.success('已安全退出')
}
</script>

<style scoped>
/* ===================================================================
   整体布局容器：100vh 全屏高度
   =================================================================== */
.app-layout {
  height: 100vh;
  overflow: hidden;
}

/* ===================================================================
   侧边栏：深色主题，宽度动画过渡
   =================================================================== */
.sidebar {
  /* 折叠/展开时宽度变化的过渡动画（0.25s cubic-bezier 曲线更流畅） */
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  background: #1a1d23;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止折叠动画过程中内容溢出 */
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
}

/* ——— Logo 区域 ——— */
.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap; /* 防止折叠时文字换行 */
  overflow: hidden;
  flex-shrink: 0;
}

/* 折叠时 Logo 区域居中对齐 */
.sidebar-logo--collapsed {
  justify-content: center;
  padding: 0;
}

/* Logo 图标圆形背景：青绿色渐变 */
.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: linear-gradient(135deg, #11998e, #38ef7d);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 10px rgba(17, 153, 142, 0.4);
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

/* ——— 侧边栏导航菜单 ——— */
.sidebar-menu {
  flex: 1;
  border-right: none; /* 覆盖 Element Plus 默认的右边框 */
  background: transparent; /* 使用父元素 .sidebar 的背景色 */
  padding: 8px 0;
}

/* 覆写 Element Plus 菜单的颜色变量 */
.sidebar-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #a8b2c1;
  --el-menu-active-color: #fff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
}

/* 激活菜单项：青绿色渐变背景 */
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(17, 153, 142, 0.3), rgba(56, 239, 125, 0.15)) !important;
  color: #38ef7d !important;
  border-right: 3px solid #38ef7d;
}

/* 悬停状态 */
.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #e0e6ed !important;
}

/* 每个菜单项基础样式 */
.menu-item {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}

/* 折叠后菜单项居中（el-aside 折叠时自动处理） */
.sidebar-menu.el-menu--collapse {
  width: 64px;
}

/* ===================================================================
   右侧主容器
   =================================================================== */
.main-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ===================================================================
   顶部 Header：白色背景，左右布局
   =================================================================== */
.app-header {
  height: 60px !important; /* 覆盖 el-header 默认 60px，确保与侧边栏对齐 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #f0f2f5;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  gap: 16px;
}

/* 左侧：折叠按钮 + 面包屑 */
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 折叠按钮：hover 时变色 */
.collapse-btn {
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
  flex-shrink: 0;
}

.collapse-btn:hover {
  color: #11998e;
}

/* 面包屑字体大小 */
.breadcrumb :deep(.el-breadcrumb__item) {
  font-size: 13px;
}

/* 右侧：用户信息区 */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 用户名显示 */
.username-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

/* 头像：青绿渐变背景 */
.user-avatar {
  cursor: pointer;
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  transition: box-shadow 0.2s;
}

.user-avatar:hover {
  box-shadow: 0 0 0 3px rgba(17, 153, 142, 0.2);
}

/* ===================================================================
   主内容区：灰色背景，内边距，可滚动
   =================================================================== */
.app-main {
  flex: 1;
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>

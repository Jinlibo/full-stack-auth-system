<template>
  <!-- ============================================================
       主布局组件（Layout）
       结构：左侧侧边栏（Sidebar） + 右侧主区域（Header + Main）
       所有需要认证的页面路由都嵌套在此组件的 <router-view> 中
       ============================================================ -->
  <el-container class="layout-root">

    <!-- ==============================
         左侧侧边栏
         - 支持折叠/展开（isCollapse 控制）
         - 宽度通过动态绑定实现平滑过渡
         ============================== -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">

      <!-- Logo 区域：展开时显示图标+文字，折叠时仅显示图标居中 -->
      <div :class="{ collapsed: isCollapse }" class="sidebar-logo">
        <!-- 自定义六边形 SVG 图标（品牌标识） -->
        <div class="logo-icon">
          <svg fill="none" height="28" viewBox="0 0 48 48" width="28">
            <!-- 外圆：半透明白色背景 -->
            <circle cx="24" cy="24" fill="rgba(255,255,255,0.15)" r="24"/>
            <!-- 六边形框线：代表模块化/安全 -->
            <path d="M24 10 L36 17 L36 31 L24 38 L12 31 L12 17 Z" fill="none" stroke="white" stroke-width="2.5"/>
            <!-- 中心圆点：代表核心认证节点 -->
            <circle cx="24" cy="24" fill="white" r="5"/>
          </svg>
        </div>
        <!-- 平台名称：折叠时通过 v-if 隐藏（避免文字溢出） -->
        <span v-if="!isCollapse" class="logo-text">认证授权平台</span>
      </div>

      <!-- ——— 导航菜单 ——— -->
      <!-- :collapse="isCollapse"：控制折叠状态，折叠时只显示图标（el-menu 内置行为） -->
      <!-- :default-active="$route.path"：当前路由路径对应的菜单项自动高亮 -->
      <!-- router：开启路由模式，点击菜单项会自动导航到 index 指定的路径 -->
      <el-menu
          :collapse="isCollapse"
          :collapse-transition="false"
          :default-active="$route.path"
          class="sidebar-menu"
          router
      >
        <!-- 仪表盘菜单项 -->
        <el-menu-item index="/dashboard">
          <el-icon>
            <DataAnalysis/>
          </el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>

        <!-- 分组标题"系统管理"：折叠时隐藏（无空间显示文字） -->
        <div v-if="!isCollapse" class="menu-group-label">系统管理</div>

        <!-- 用户管理 -->
        <el-menu-item index="/system/user">
          <el-icon>
            <User/>
          </el-icon>
          <template #title>用户管理</template>
        </el-menu-item>

        <!-- 角色管理 -->
        <el-menu-item index="/system/role">
          <el-icon>
            <UserFilled/>
          </el-icon>
          <template #title>角色管理</template>
        </el-menu-item>

        <!-- 权限管理 -->
        <el-menu-item index="/system/permission">
          <el-icon>
            <Key/>
          </el-icon>
          <template #title>权限管理</template>
        </el-menu-item>

        <!-- 分组标题"产品管理" -->
        <div v-if="!isCollapse" class="menu-group-label">产品管理</div>

        <!-- 产品列表 -->
        <el-menu-item index="/product/list">
          <el-icon>
            <Grid/>
          </el-icon>
          <template #title>产品列表</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- ==============================
         右侧主区域（顶部导航 + 内容区）
         ============================== -->
    <el-container>

      <!-- ——— 顶部导航栏 Header ——— -->
      <el-header class="header">
        <!-- 左侧：折叠按钮 + 面包屑导航 -->
        <div class="header-left">
          <!-- 折叠/展开侧边栏按钮：点击切换 isCollapse 状态 -->
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <!-- 展开状态显示 Fold（向左折叠）图标 -->
            <Fold v-if="!isCollapse"/>
            <!-- 折叠状态显示 Expand（向右展开）图标 -->
            <Expand v-else/>
          </el-icon>

          <!-- 面包屑导航：首页 > 当前页面名称 -->
          <!-- currentMenu 是计算属性，根据当前路由路径映射到菜单标题 -->
          <el-breadcrumb class="breadcrumb" separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentMenu">{{ currentMenu }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 右侧：个人中心快捷入口 + 用户下拉菜单 -->
        <div class="header-right">
          <!-- 个人中心快捷图标按钮 -->
          <el-tooltip content="个人中心" placement="bottom">
            <el-icon class="header-action" @click="$router.push('/profile')">
              <UserFilled/>
            </el-icon>
          </el-tooltip>

          <!-- 竖向分隔线 -->
          <el-divider direction="vertical"/>

          <!-- 用户信息下拉菜单（点击触发） -->
          <el-dropdown trigger="click">
            <!-- 触发元素：头像 + 用户名 + 下拉箭头 -->
            <div class="user-info">
              <!-- 用户头像：渐变背景，显示昵称首字母 -->
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo?.nickname?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <!-- 用户名称：超长时截断显示 -->
              <span class="user-name">{{ userStore.userInfo?.nickname }}</span>
              <!-- 下拉箭头图标 -->
              <el-icon style="font-size: 12px; color: #909399;">
                <ArrowDown/>
              </el-icon>
            </div>

            <!-- 下拉菜单内容 -->
            <template #dropdown>
              <el-dropdown-menu>
                <!-- 个人中心入口 -->
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon>
                    <UserFilled/>
                  </el-icon>
                  个人中心
                </el-dropdown-item>
                <!-- 退出登录：divided=true 在上方添加分隔线 -->
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

      <!-- ——— 主内容区域：渲染当前路由对应的页面组件 ——— -->
      <el-main class="main-content">
        <!-- <router-view> 是 Vue Router 的核心出口，渲染当前激活的子路由组件 -->
        <router-view/>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
/**
 * 主布局组件 - 脚本逻辑
 *
 * 职责：
 *  1. 控制侧边栏折叠状态（isCollapse）
 *  2. 根据当前路由路径计算面包屑标题（currentMenu）
 *  3. 处理退出登录操作（先调接口再清本地数据）
 */
import {ref, computed} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {useUserStore} from '../store/user'
import {logout} from '../api/auth'
import {ElMessage} from 'element-plus'

/** 侧边栏折叠状态：true=折叠，false=展开（默认展开） */
const isCollapse = ref(false)

/** Vue Router 实例：用于编程式导航（如退出登录后跳转到登录页） */
const router = useRouter()

/** 当前激活路由信息：用于获取 path 以确定面包屑标题 */
const route = useRoute()

/** 用户状态管理仓库：读取昵称和用户名 */
const userStore = useUserStore()

/**
 * 路由路径 → 菜单标题的映射表
 * 用于面包屑导航中显示当前页面的中文名称
 * 每次新增路由时，需要在此处同步添加映射关系
 */
const menuTitles = {
  '/dashboard': '仪表盘',
  '/system/user': '用户管理',
  '/system/role': '角色管理',
  '/system/permission': '权限管理',
  '/product/list': '产品列表',
  '/profile': '个人中心',
}

/**
 * 当前菜单标题（计算属性）
 * 响应式：当 route.path 变化时自动重新计算
 * 若路径不在映射表中，返回 undefined（面包屑不显示第二级）
 */
const currentMenu = computed(() => menuTitles[route.path])

/**
 * 退出登录处理函数
 *
 * 流程：
 *  1. 调用后端 logout 接口，将当前 Token 加入 Redis 黑名单
 *     （使用 try/catch 容错：即使接口失败，客户端也要清除本地 token）
 *  2. 调用 userStore.logout() 清除内存中的用户信息和 localStorage 中的 token
 *  3. 跳转到登录页
 *  4. 显示退出成功的提示消息
 */
const handleLogout = async () => {
  try {
    // 通知服务器将当前 Token 加入黑名单（防止 Token 泄露后被继续使用）
    await logout()
  } catch (e) {
    // 网络错误时静默忽略，依然执行客户端清理
    // 即使服务器注销失败，Token 也会在有效期结束后自动失效
  }
  // 清除本地状态（Pinia store + localStorage）
  userStore.logout()
  // 跳转到登录页
  router.push('/login')
  // 显示成功提示
  ElMessage.success('已安全退出登录')
}
</script>

<style scoped>
.layout-root {
  height: 100vh;
  overflow: hidden;
  background-color: #e4e4e7;
}

.sidebar {
  background: #09090b; /* Very dark zinc */
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.4);
  z-index: 100;
}

.sidebar-logo {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
  white-space: nowrap;
  flex-shrink: 0;
  background: linear-gradient(to bottom, rgba(255,255,255,0.02), transparent);
}

.sidebar-logo.collapsed { justify-content: center; padding: 0; }

.logo-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.4);
}
.logo-icon svg { width: 22px; height: 22px; }

.logo-text { font-size: 16px; font-weight: 800; color: #fff; letter-spacing: 0.5px; }

.sidebar-menu {
  background: transparent !important;
  border: none !important;
  flex: 1; overflow-y: auto; overflow-x: hidden;
  padding: 12px 8px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  color: #a1a1aa !important;
  height: 46px; line-height: 46px;
  margin: 4px 12px; border-radius: 12px;
  padding-left: 16px !important;
  font-weight: 500;
  display: flex; align-items: center;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #e4e4e7 !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.15), rgba(139, 92, 246, 0.15)) !important;
  color: #a78bfa !important;
  font-weight: 700;
  box-shadow: inset 3px 0 0 #8b5cf6;
}

.sidebar-menu :deep(.el-menu-item.is-active .el-icon) { color: #a78bfa !important; }

/* 折叠状态的精准偏移 */
.sidebar-menu.el-menu--collapse :deep(.el-menu-item) { 
  margin: 4px 8px !important; 
  padding-left: 12px !important;
  width: 48px !important;
}
.sidebar-menu.el-menu--collapse :deep(.el-menu-tooltip__trigger) { 
  padding: 0 !important; 
  display: flex !important; 
  align-items: center !important; 
}

.menu-group-label {
  font-size: 12px; font-weight: 700;
  color: #52525b; text-transform: uppercase; letter-spacing: 1px;
  padding: 24px 20px 8px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  opacity: 1; transition: opacity 0.3s;
}

.header {
  height: 64px !important;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid #d4d4d8;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
  flex-shrink: 0; z-index: 10;
}

.header-left { display: flex; align-items: center; gap: 20px; }

.collapse-btn {
  font-size: 20px; color: #52525b; cursor: pointer; padding: 8px;
  border-radius: 8px; transition: all 0.2s; background: #e4e4e7;
}
.collapse-btn:hover { background: #d4d4d8; color: #18181b; }

.breadcrumb :deep(.el-breadcrumb__item) { font-size: 14px; font-weight: 500; }

.header-right { display: flex; align-items: center; gap: 16px; }

.header-action {
  font-size: 18px; color: #52525b; cursor: pointer; padding: 8px;
  border-radius: 8px; transition: all 0.2s; background: #e4e4e7;
}
.header-action:hover { background: #d4d4d8; color: #18181b; }

.user-info {
  display: flex; align-items: center; gap: 10px; cursor: pointer;
  padding: 6px 12px 6px 6px; border-radius: 24px; transition: all 0.2s;
  background: #e4e4e7; border: 1px solid #d4d4d8;
}
.user-info:hover { background: #d4d4d8; border-color: #a1a1aa;}

.user-avatar {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6); color: #fff;
  font-weight: 700; font-size: 14px; box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
}

.user-name { font-size: 14px; color: #18181b; font-weight: 600; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.main-content {
  background: #e4e4e7; padding: 24px; overflow-y: auto;
}
</style>

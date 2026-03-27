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

        <!-- 系统管理：角色管理 + 权限管理 -->
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Setting/></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item class="menu-item" index="/system/roles">角色管理</el-menu-item>
          <el-menu-item class="menu-item" index="/system/permissions">权限管理</el-menu-item>
        </el-sub-menu>

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
.app-layout { height: 100vh; overflow: hidden; background: #f1f5f9;}

.sidebar {
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: #ffffff;
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.03);
  z-index: 100; border-right: 1px solid #e2e8f0;
}

.sidebar-logo {
  height: 72px; display: flex; align-items: center; gap: 12px;
  padding: 0 20px; border-bottom: 1px solid #f1f5f9;
  white-space: nowrap; overflow: hidden; flex-shrink: 0;
}

.sidebar-logo--collapsed { justify-content: center; padding: 0; }

.logo-icon {
  width: 38px; height: 38px; border-radius: 12px;
  background: linear-gradient(135deg, #14b8a6, #0ea5e9);
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0; box-shadow: 0 6px 16px rgba(20, 184, 166, 0.3);
}

.logo-text { font-size: 17px; font-weight: 800; color: #0f172a; letter-spacing: -0.2px; }

.sidebar-menu {
  flex: 1; border-right: none; background: transparent; padding: 16px 0;
}

.sidebar-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #64748b;
  --el-menu-active-color: #0f172a;
  --el-menu-hover-bg-color: #f8fafc;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(20, 184, 166, 0.1), rgba(14, 165, 233, 0.05)) !important;
  color: #0ea5e9 !important; font-weight: 700; border-radius: 12px; box-shadow: inset 3px 0 0 #0ea5e9;
}

.sidebar-menu :deep(.el-menu-item:hover) { background: #f8fafc !important; color: #334155 !important; border-radius: 12px;}

.menu-item { margin: 4px 12px; border-radius: 12px; height: 48px; line-height: 48px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.sidebar-menu.el-menu--collapse :deep(.el-menu-item),
.sidebar-menu.el-menu--collapse :deep(.el-sub-menu__title) {
  margin: 4px 8px !important;
  padding-left: 12px !important;
  width: 48px !important;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  color: #64748b !important; border-radius: 12px; margin: 4px 12px; height: 48px; line-height: 48px; font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.sidebar-menu :deep(.el-sub-menu__title:hover) { background: #f8fafc !important; color: #334155 !important; }
.sidebar-menu :deep(.el-menu--popup) { background: #ffffff !important; box-shadow: 0 10px 30px rgba(0,0,0,0.1) !important; border-radius: 16px; padding: 8px;}
.sidebar-menu :deep(.el-sub-menu .el-menu-item) { color: #64748b; background: transparent; padding-left: 48px !important; border-radius: 10px; margin: 4px 12px; }
.sidebar-menu :deep(.el-sub-menu .el-menu-item:hover) { background: #f8fafc !important; color: #0ea5e9 !important; }
.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active) { background: rgba(14, 165, 233, 0.08) !important; color: #0ea5e9 !important; font-weight: 700;}

.main-container { flex: 1; overflow: hidden; display: flex; flex-direction: column; background: #f1f5f9;}

.app-header {
  height: 72px !important; display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px; background: rgba(255,255,255,0.8); backdrop-filter: blur(20px);
  border-bottom: 1px solid #e2e8f0; box-shadow: 0 4px 12px rgba(0,0,0,0.02);
  flex-shrink: 0; gap: 16px; z-index: 10;
}

.header-left { display: flex; align-items: center; gap: 20px; }

.collapse-btn {
  cursor: pointer; color: #64748b; transition: all 0.2s; flex-shrink: 0;
  background: #f1f5f9; padding: 10px; border-radius: 10px;
}
.collapse-btn:hover { color: #0ea5e9; background: #e0f2fe; }

.breadcrumb :deep(.el-breadcrumb__item) { font-size: 14px; font-weight: 600; }
.breadcrumb :deep(.el-breadcrumb__inner) { color: #64748b; }

.header-right { display: flex; align-items: center; gap: 16px; }

.username-label { font-size: 15px; font-weight: 700; color: #0f172a; white-space: nowrap; }

.user-avatar {
  cursor: pointer; background: linear-gradient(135deg, #14b8a6, #0ea5e9); color: #fff;
  font-weight: 700; font-size: 16px; transition: all 0.2s; box-shadow: 0 4px 12px rgba(20, 184, 166, 0.3);
}
.user-avatar:hover { transform: scale(1.05); }

.app-main { flex: 1; background: transparent; padding: 24px; overflow-y: auto; }
</style>

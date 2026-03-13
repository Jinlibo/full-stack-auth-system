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
/* ===================================================================
   整体布局容器：充满视口高度，禁止滚动（内部各区域自己管理滚动）
   =================================================================== */
.layout-root {
  height: 100vh;
  overflow: hidden;
}

/* ===================================================================
   侧边栏（Sidebar）
   =================================================================== */
.sidebar {
  /* 深色渐变背景：上深下浅的竖向渐变，营造层次感 */
  background: linear-gradient(180deg, #1a1c2e 0%, #2d2f4a 100%);
  /* 宽度切换时的平滑动画（cubic-bezier：Material Design 标准缓动函数） */
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden; /* 折叠时隐藏超出部分（如文字标签） */
  display: flex;
  flex-direction: column;
  /* 右侧投影：营造侧边栏浮起效果，区分侧边栏与内容区 */
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
}

/* Logo 区域 */
.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  /* 底部细线分隔 Logo 和菜单区域 */
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
  white-space: nowrap; /* 防止平台名称换行 */
  flex-shrink: 0; /* 不因内容区高度压缩而缩小 */
}

/* Logo 区域折叠状态：图标居中 */
.sidebar-logo.collapsed {
  justify-content: center;
  padding: 0;
}

/* 平台名称文字：白色、加粗 */
.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

/* ——— 导航菜单样式 ——— */
.sidebar-menu {
  background: transparent !important; /* 覆盖 el-menu 的默认白色背景 */
  border: none !important; /* 移除 el-menu 的右侧边框 */
  flex: 1; /* 占满剩余高度 */
  overflow-y: auto; /* 菜单项过多时可滚动 */
  overflow-x: hidden;
  padding: 8px 0;
}

/* 菜单项默认样式：半透明白色文字，圆角 */
.sidebar-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.65) !important;
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 8px;
  transition: all 0.18s;
}

/* 菜单项 hover：增亮背景和文字 */
.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff !important;
}

/* 当前激活菜单项：渐变高亮背景 + 紫色文字 */
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.35), rgba(118, 75, 162, 0.25)) !important;
  color: #a78bfa !important;
  font-weight: 600;
}

/* 激活菜单项图标颜色 */
.sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #a78bfa !important;
}

/* 折叠状态下菜单项的水平内边距缩小 */
.sidebar-menu :deep(.el-menu--collapse .el-menu-item) {
  margin: 2px 4px;
}

/* 菜单分组标签（"系统管理"/"产品管理"） */
.menu-group-label {
  font-size: 11px;
  font-weight: 600;
  /* 很低透明度的白色：不抢主菜单的视觉注意力 */
  color: rgba(255, 255, 255, 0.25);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 16px 18px 6px;
}

/* ===================================================================
   顶部导航栏（Header）
   =================================================================== */
.header {
  height: 60px !important; /* !important 覆盖 el-header 的默认高度 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  /* 轻微底部阴影，区分 header 和内容区 */
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
  flex-shrink: 0; /* 防止被内容区压缩 */
}

/* Header 左侧：折叠按钮 + 面包屑，横向排列 */
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 折叠/展开按钮 */
.collapse-btn {
  font-size: 20px;
  color: #606266;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: background 0.15s;
}

.collapse-btn:hover {
  background: #f5f5f5;
  color: #409eff;
}

/* 面包屑字体大小 */
.breadcrumb {
  font-size: 13px;
}

/* Header 右侧：个人中心图标 + 分隔线 + 用户下拉菜单 */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 个人中心图标按钮 */
.header-action {
  font-size: 18px;
  color: #606266;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: background 0.15s;
}

.header-action:hover {
  background: #f5f5f5;
  color: #409eff;
}

/* 用户信息区域（头像+名称+箭头） */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: background 0.15s;
}

.user-info:hover {
  background: #f5f5f5;
}

/* 用户头像：渐变背景（与 Logo 颜色一致，保持视觉统一） */
.user-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  flex-shrink: 0;
}

/* 用户名称：超长时省略 */
.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===================================================================
   主内容区域（Main）
   =================================================================== */
.main-content {
  background: #f4f6fb; /* 浅灰蓝色背景，与白色卡片形成对比 */
  padding: 24px;
  overflow-y: auto; /* 内容过长时垂直滚动（而非整页滚动） */
}
</style>

<template>
  <!-- ============================================================
       仪表盘页面（Dashboard）
       功能：展示系统概览统计卡片、当前用户登录信息、快捷入口导航
       ============================================================ -->
  <div class="dashboard">

    <!-- ——— 欢迎横幅：显示用户名和当前日期 ——— -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <!-- 优先显示昵称，无昵称则显示用户名；用户信息可能在刷新后延迟加载 -->
        <h2>你好，{{ userStore.userInfo?.nickname || userStore.userInfo?.username }} 👋</h2>
        <p>欢迎回到认证授权平台管理后台，今天是 {{ today }}</p>
      </div>
      <!-- 右侧大头像：显示昵称/用户名的首字母 -->
      <div class="welcome-avatar">
        <el-avatar :size="64" class="big-avatar">
          {{
            userStore.userInfo?.nickname?.charAt(0)?.toUpperCase()
            || userStore.userInfo?.username?.charAt(0)?.toUpperCase()
          }}
        </el-avatar>
      </div>
    </div>

    <!-- ——— 统计数据卡片行：用户数、角色数、产品数、权限数 ——— -->
    <!-- :gutter="20"：列间距 20px；响应式：xs(手机)=2列，sm(平板)以上=4列 -->
    <el-row :gutter="20" style="margin-bottom: 24px;">
      <el-col
          v-for="stat in stats"
          :key="stat.label"
          :sm="6"
          :xs="12"
      >
        <!-- 每个统计卡片，使用 CSS 变量 --accent 传递主题色 -->
        <div :style="`--accent: ${stat.color}`" class="stat-card">
          <!-- 左侧图标区：圆角正方形，背景为主题色的 15% 透明度 -->
          <div class="stat-icon-wrap">
            <!-- 动态组件：根据 stat.icon 字符串（如 'User'）渲染对应的 Element Plus 图标 -->
            <el-icon :size="24">
              <component :is="stat.icon"/>
            </el-icon>
          </div>
          <!-- 右侧数值和标签 -->
          <div class="stat-body">
            <!-- 统计数值：null 时显示 '--'（数据加载前的占位符） -->
            <div class="stat-value">{{ stat.value ?? '--' }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ——— 信息行：当前用户信息 + 快捷入口 ——— -->
    <el-row :gutter="20">

      <!-- 左列：当前登录用户详情（用户名、昵称、邮箱、角色） -->
      <el-col :md="12" :xs="24">
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header-row">
              <el-icon>
                <UserFilled/>
              </el-icon>
              当前登录信息
            </div>
          </template>
          <!-- el-descriptions：用于展示键值对信息，column=1 表示单列布局 -->
          <el-descriptions :border="false" :column="1" class="info-desc">
            <!-- 用户名：使用标签高亮显示 -->
            <el-descriptions-item label="用户名">
              <el-tag>{{ userStore.userInfo?.username }}</el-tag>
            </el-descriptions-item>
            <!-- 昵称：若无则显示 '-' -->
            <el-descriptions-item label="昵称">
              {{ userStore.userInfo?.nickname || '-' }}
            </el-descriptions-item>
            <!-- 邮箱 -->
            <el-descriptions-item label="邮箱">
              {{ userStore.userInfo?.email || '-' }}
            </el-descriptions-item>
            <!-- 角色：以小标签列表展示，无角色时显示提示文本 -->
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
                <!-- 无角色时的占位文本 -->
                <span
                    v-if="!userStore.userInfo?.roles?.length"
                    style="color: #c0c4cc;"
                >暂无角色</span>
              </el-space>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右列：快捷入口，点击直接跳转到对应管理页面 -->
      <el-col :md="12" :xs="24">
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header-row">
              <el-icon>
                <DataAnalysis/>
              </el-icon>
              快捷入口
            </div>
          </template>
          <!-- 2×2 网格布局的快捷入口卡片 -->
          <div class="quick-links">
            <div
                v-for="link in quickLinks"
                :key="link.path"
                class="quick-link-item"
                @click="$router.push(link.path)"
            >
              <!-- 图标区：带有半透明背景色 -->
              <div :style="`background: ${link.bg}`" class="quick-link-icon">
                <el-icon :size="20">
                  <component :is="link.icon"/>
                </el-icon>
              </div>
              <span>{{ link.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 仪表盘页面 - 脚本逻辑
 *
 * 当前版本：统计数值（用户总数、角色数等）暂时使用 null 占位，
 * 显示为 '--'。后续可调用各管理模块的统计接口填充实际数据。
 *
 * 架构说明：
 *  - 从 userStore 获取当前登录用户信息（已在路由守卫中加载）
 *  - stats：响应式数组，后续可通过接口更新 value 字段
 *  - quickLinks：静态配置，点击后由 $router.push 跳转
 */
import {ref} from 'vue'
import {useUserStore} from '../store/user'

/** 用户状态管理仓库，从中读取当前登录用户的昵称、用户名、角色、邮箱等 */
const userStore = useUserStore()

/**
 * 当前日期字符串（本地化格式）
 * 格式示例："2026年3月14日星期六"
 * toLocaleDateString 使用 'zh-CN' locale，参数 year/month/day/weekday 控制显示内容
 */
const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
})

/**
 * 统计卡片数据配置
 * - label：卡片下方的描述文字
 * - value：统计数值（null 时显示 '--'，可后续通过接口填充）
 * - icon：Element Plus 图标组件名（字符串，由 <component :is=""> 动态渲染）
 * - color：主题色（用于图标背景和数值颜色，CSS 变量 --accent）
 */
const stats = ref([
  {label: '用户总数', value: null, icon: 'User', color: '#667eea'},
  {label: '角色数量', value: null, icon: 'UserFilled', color: '#67C23A'},
  {label: '产品数量', value: null, icon: 'Box', color: '#E6A23C'},
  {label: '权限数量', value: null, icon: 'Key', color: '#F56C6C'},
])

/**
 * 快捷入口配置列表
 * - label：入口显示名称
 * - path：点击后的跳转路由路径（对应 router/index.js 中的路由）
 * - icon：Element Plus 图标组件名
 * - bg：图标区背景色（半透明主题色）
 */
const quickLinks = [
  {label: '用户管理', path: '/system/user', icon: 'User', bg: 'rgba(102,126,234,0.12)'},
  {label: '角色管理', path: '/system/role', icon: 'UserFilled', bg: 'rgba(103,194,58,0.12)'},
  {label: '产品管理', path: '/product/list', icon: 'Grid', bg: 'rgba(230,162,60,0.12)'},
  {label: '权限管理', path: '/system/permission', icon: 'Key', bg: 'rgba(245,108,108,0.12)'},
]
</script>

<style scoped>
/* ===================================================================
   仪表盘主容器：flex 竖向布局
   =================================================================== */
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ===================================================================
   欢迎横幅：渐变背景、白色文字、圆角
   =================================================================== */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.25);
}

/* 欢迎文字：标题 + 副标题 */
.welcome-text h2 {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px;
}

.welcome-text p {
  font-size: 14px;
  opacity: 0.8;
  margin: 0;
}

/* 右侧大头像：半透明白色背景 */
.big-avatar {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-weight: 700;
  font-size: 28px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* ===================================================================
   统计卡片：白色背景、圆角、hover 浮起效果
   =================================================================== */
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #f0f0f0;
  transition: box-shadow 0.2s, transform 0.2s;
  cursor: default;
}

.stat-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

/* 图标区：使用 CSS 变量 --accent 的 15% 透明度作为背景，主题色作为图标颜色 */
.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  /* color-mix：CSS 新特性，将 accent 与透明混合，IE/旧版 Edge 不支持 */
  background: color-mix(in srgb, var(--accent) 15%, transparent);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 统计数值：大字号、深色 */
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
}

/* 统计标签：小字、灰色 */
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 3px;
}

/* ===================================================================
   信息卡片：圆角、无 box-shadow（shadow="never"）、细边框
   =================================================================== */
.info-card {
  border-radius: 12px !important;
  border: 1px solid #f0f0f0 !important;
}

/* 卡片头部内边距 */
.info-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
}

/* 卡片头部行：图标 + 文字横向排列 */
.card-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

/* el-descriptions label 列：固定宽度、灰色 */
.info-desc :deep(.el-descriptions__label) {
  width: 70px;
  color: #909399;
  font-size: 13px;
}

/* el-descriptions content 列 */
.info-desc :deep(.el-descriptions__content) {
  font-size: 13px;
  color: #303133;
}

/* ===================================================================
   快捷入口：2×2 网格
   =================================================================== */
.quick-links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 4px 0;
}

/* 单个快捷入口项：圆角背景、hover 效果 */
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
  background: #f0f2ff;
  border-color: #c5caf5;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
}

/* 快捷入口图标区：小圆角正方形 */
.quick-link-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: inherit;
  flex-shrink: 0;
}
</style>

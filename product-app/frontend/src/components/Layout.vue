<template>
  <el-container style="height: 100vh">
    <el-aside style="background: #1d1e1f;" width="220px">
      <div
          style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; font-weight: bold;">
        产品应用
      </div>
      <el-menu :default-active="$route.path" active-text-color="#409EFF" background-color="#1d1e1f" router
               style="border: none" text-color="#bfcbd9">
        <el-menu-item index="/dashboard">
          <el-icon>
            <DataAnalysis/>
          </el-icon>
          <span>仪表盘</span></el-menu-item>
        <el-menu-item index="/users">
          <el-icon>
            <User/>
          </el-icon>
          <span>用户管理</span></el-menu-item>
        <el-menu-item index="/profile">
          <el-icon>
            <UserFilled/>
          </el-icon>
          <span>个人中心</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header
          style="display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #eee; padding: 0 20px; gap: 16px;">
        <span>{{ userStore.userInfo?.nickname }}</span>
        <el-dropdown>
          <el-avatar :size="36">{{ userStore.userInfo?.nickname?.charAt(0) }}</el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 20px;">
        <router-view/>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import {useRouter} from 'vue-router'
import {useUserStore} from '../store/user'
import {logout} from '../api/auth'
import {ElMessage} from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const handleLogout = async () => {
  try {
    await logout()
  } catch (e) {
  }
  userStore.logout();
  router.push('/login');
  ElMessage.success('已退出')
}
</script>

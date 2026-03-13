<template>
  <el-container style="height: 100vh">
    <el-aside :width="isCollapse ? '64px' : '220px'" style="background: #304156; transition: width .3s">
      <div
          style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold;">
        <span v-if="!isCollapse">认证授权平台</span>
        <span v-else>AP</span>
      </div>
      <el-menu :collapse="isCollapse" :default-active="$route.path" active-text-color="#409EFF"
               background-color="#304156" router style="border: none"
               text-color="#bfcbd9">
        <el-menu-item index="/dashboard">
          <el-icon>
            <DataAnalysis/>
          </el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-sub-menu index="system">
          <template #title>
            <el-icon>
              <Setting/>
            </el-icon>
            <span>系统管理</span></template>
          <el-menu-item index="/system/user">
            <el-icon>
              <User/>
            </el-icon>
            用户管理
          </el-menu-item>
          <el-menu-item index="/system/role">
            <el-icon>
              <UserFilled/>
            </el-icon>
            角色管理
          </el-menu-item>
          <el-menu-item index="/system/permission">
            <el-icon>
              <Key/>
            </el-icon>
            权限管理
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="product">
          <template #title>
            <el-icon>
              <Box/>
            </el-icon>
            <span>产品管理</span></template>
          <el-menu-item index="/product/list">
            <el-icon>
              <Grid/>
            </el-icon>
            产品列表
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header
          style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee; padding: 0 20px;">
        <el-icon style="cursor: pointer; font-size: 20px" @click="isCollapse = !isCollapse">
          <Fold/>
        </el-icon>
        <div style="display: flex; align-items: center; gap: 16px;">
          <span>{{ userStore.userInfo?.nickname }}</span>
          <el-dropdown>
            <el-avatar :size="36" :src="userStore.userInfo?.avatar || ''">
              {{ userStore.userInfo?.nickname?.charAt(0) }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main style="background: #f5f5f5; padding: 20px;">
        <router-view/>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useUserStore} from '../store/user'
import {logout} from '../api/auth'
import {ElMessage} from 'element-plus'

const isCollapse = ref(false)
const router = useRouter()
const userStore = useUserStore()

const handleLogout = async () => {
  try {
    await logout()
  } catch (e) { /* ignore */
  }
  userStore.logout()
  router.push('/login')
  ElMessage.success('已退出登录')
}
</script>

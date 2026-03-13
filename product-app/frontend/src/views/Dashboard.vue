<template>
  <div>
    <el-card>
      <template #header>欢迎使用产品应用</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userStore.userInfo?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ userStore.userInfo?.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag v-for="r in userStore.userInfo?.roles" :key="r" style="margin-right: 4px;">{{ r }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="OAuth绑定">
          <template v-if="userStore.userInfo?.oauthBindings?.length">
            <el-tag v-for="b in userStore.userInfo.oauthBindings" :key="b.provider" style="margin-right: 4px;"
                    type="success">
              {{ b.provider }}: {{ b.oauthUsername }}
            </el-tag>
          </template>
          <span v-else style="color: #999;">未绑定</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import {useUserStore} from '../store/user'

const userStore = useUserStore()
</script>

<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>用户总数</template>
          <div style="font-size: 32px; font-weight: bold; text-align: center; color: #409EFF;">--</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>角色数量</template>
          <div style="font-size: 32px; font-weight: bold; text-align: center; color: #67C23A;">--</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>产品数量</template>
          <div style="font-size: 32px; font-weight: bold; text-align: center; color: #E6A23C;">--</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>权限数量</template>
          <div style="font-size: 32px; font-weight: bold; text-align: center; color: #F56C6C;">--</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top: 20px">
      <template #header>欢迎使用认证授权平台</template>
      <p>当前登录用户: {{ userStore.userInfo?.nickname }} ({{ userStore.userInfo?.username }})</p>
      <p>角色: {{ userStore.userInfo?.roles?.join(', ') }}</p>
    </el-card>
  </div>
</template>

<script setup>
import {useUserStore} from '../store/user'

const userStore = useUserStore()
</script>

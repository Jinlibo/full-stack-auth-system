<template>
  <div class="error-bg">
    <el-card class="error-card" shadow="always">
      <div class="error-content">
        <el-icon :size="64" style="color: #F56C6C;">
          <CircleCloseFilled/>
        </el-icon>
        <div class="error-title">授权失败</div>
        <div class="error-msg">{{ message }}</div>
        <el-button style="margin-top: 24px;" type="primary" @click="goBack">
          返回上一页
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import {computed} from 'vue'
import {useRoute} from 'vue-router'

const route = useRoute()
const message = computed(() =>
    route.query.message || route.query.error_description || '授权过程中发生错误，请重试。'
)

const goBack = () => window.history.back()
</script>

<style scoped>
.error-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.error-card {
  width: 420px;
  text-align: center;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
}

.error-title {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
  margin-top: 16px;
}

.error-msg {
  font-size: 14px;
  color: #606266;
  margin-top: 12px;
  line-height: 1.6;
}
</style>

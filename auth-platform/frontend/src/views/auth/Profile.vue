<template>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-card>
        <template #header>个人信息</template>
        <el-form :model="form" label-width="80px">
          <el-form-item label="用户名">
            <el-input :model-value="userStore.userInfo?.username" disabled/>
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname"/>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email"/>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone"/>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSave">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card>
        <template #header>修改密码</template>
        <el-form :model="pwdForm" label-width="80px">
          <el-form-item label="旧密码">
            <el-input v-model="pwdForm.oldPassword" show-password type="password"/>
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" show-password type="password"/>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleChangePwd">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import {reactive, onMounted} from 'vue'
import {useUserStore} from '../../store/user'
import {updateProfile, changePassword} from '../../api/user'
import {ElMessage} from 'element-plus'

const userStore = useUserStore()
const form = reactive({nickname: '', email: '', phone: ''})
const pwdForm = reactive({oldPassword: '', newPassword: ''})

onMounted(() => {
  if (userStore.userInfo) {
    form.nickname = userStore.userInfo.nickname || ''
    form.email = userStore.userInfo.email || ''
    form.phone = userStore.userInfo.phone || ''
  }
})

const handleSave = async () => {
  await updateProfile(form)
  ElMessage.success('保存成功')
  await userStore.fetchUserInfo()
}

const handleChangePwd = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整');
    return
  }
  await changePassword(pwdForm)
  ElMessage.success('密码已修改')
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
}
</script>

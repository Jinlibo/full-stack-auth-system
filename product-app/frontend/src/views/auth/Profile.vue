<template>
  <div>
    <el-card style="margin-bottom: 20px;">
      <template #header>个人资料</template>
      <el-form :model="form" label-width="80px" style="max-width: 500px;">
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
          <el-button :loading="updating" type="primary" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>第三方账号绑定</template>

      <div v-if="oauthBindings.length" style="margin-bottom: 16px;">
        <el-space direction="vertical" fill style="width: 100%;">
          <el-card v-for="b in oauthBindings" :key="b.provider" shadow="never"
                   style="border: 1px solid #e4e7ed;">
            <el-row align="middle" justify="space-between">
              <div>
                <el-tag style="margin-right: 8px;" type="success">{{ b.provider }}</el-tag>
                <span style="color: #606266;">{{ b.oauthUsername }}</span>
              </div>
              <el-button :loading="unbinding === b.provider" size="small"
                         type="danger"
                         @click="handleUnbind(b.provider)">
                解绑
              </el-button>
            </el-row>
          </el-card>
        </el-space>
      </div>
      <el-empty v-else :image-size="80" description="暂未绑定任何第三方账号" style="padding: 20px 0;"/>

      <el-button :disabled="isBound('auth-platform')" type="primary" @click="handleBind">
        绑定认证平台账号
      </el-button>
      <span v-if="isBound('auth-platform')"
            style="margin-left: 10px; color: #999; font-size: 13px;">已绑定</span>
    </el-card>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useUserStore} from '../../store/user'
import {updateProfile, unbindOAuth} from '../../api/user'
import {getOAuthUrl} from '../../api/auth'

const userStore = useUserStore()

const form = reactive({nickname: '', email: '', phone: ''})
const updating = ref(false)
const unbinding = ref('')

const oauthBindings = computed(() => userStore.userInfo?.oauthBindings || [])
const isBound = (provider) => oauthBindings.value.some(b => b.provider === provider)

onMounted(async () => {
  await userStore.fetchUserInfo()
  form.nickname = userStore.userInfo?.nickname || ''
  form.email = userStore.userInfo?.email || ''
  form.phone = userStore.userInfo?.phone || ''
})

const handleSave = async () => {
  updating.value = true
  try {
    await updateProfile(form)
    await userStore.fetchUserInfo()
    ElMessage.success('保存成功')
  } catch (e) {
    // handled by interceptor
  } finally {
    updating.value = false
  }
}

const handleBind = async () => {
  try {
    const state = Math.random().toString(36).substring(7)
    sessionStorage.setItem('oauth_state', state)
    sessionStorage.setItem('oauth_action', 'bind')
    const res = await getOAuthUrl(state)
    window.location.href = res.data.authorizeUrl
  } catch (e) {
    ElMessage.error('获取授权地址失败')
  }
}

const handleUnbind = async (provider) => {
  try {
    await ElMessageBox.confirm(`确认解绑「${provider}」账号吗？`, '提示', {type: 'warning'})
    unbinding.value = provider
    await unbindOAuth(provider)
    await userStore.fetchUserInfo()
    ElMessage.success('解绑成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('解绑失败')
  } finally {
    unbinding.value = ''
  }
}
</script>

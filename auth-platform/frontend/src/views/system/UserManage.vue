<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>用户管理</span>
        <div>
          <el-input v-model="query.keyword" clearable placeholder="搜索用户" style="width: 200px; margin-right: 10px;"
                    @clear="loadUsers" @keyup.enter="loadUsers"/>
          <el-button type="primary" @click="openDialog(null)">新增用户</el-button>
        </div>
      </div>
    </template>
    <el-table v-loading="loading" :data="users" stripe>
      <el-table-column label="ID" prop="id" width="60"/>
      <el-table-column label="用户名" prop="username" width="120"/>
      <el-table-column label="昵称" prop="nickname" width="120"/>
      <el-table-column label="邮箱" prop="email"/>
      <el-table-column label="手机号" prop="phone" width="130"/>
      <el-table-column label="角色" width="180">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" style="margin: 2px;">{{ r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确认删除?" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                   :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next"
                   style="margin-top: 16px; justify-content: flex-end;" @change="loadUsers"/>

    <el-dialog v-model="dialogVisible" :title="editingUser ? '编辑用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item v-if="!editingUser" label="用户名" prop="username">
          <el-input v-model="form.username"/>
        </el-form-item>
        <el-form-item v-if="!editingUser" label="密码" prop="password">
          <el-input v-model="form.password" show-password type="password"/>
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
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in allRoles" :key="r.id" :label="r.roleName" :value="r.id"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingUser" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="submitting" type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {getUsers, createUser, updateUser, deleteUser} from '../../api/user'
import {getAllRoles} from '../../api/role'
import {ElMessage} from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingUser = ref(null)
const users = ref([])
const total = ref(0)
const allRoles = ref([])
const query = reactive({pageNum: 1, pageSize: 10, keyword: ''})
const formRef = ref(null)
const form = reactive({username: '', password: '', nickname: '', email: '', phone: '', roleIds: [], status: 1})
const rules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getUsers(query)
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  const res = await getAllRoles()
  allRoles.value = res.data
}

const openDialog = (user) => {
  editingUser.value = user
  if (user) {
    Object.assign(form, {
      nickname: user.nickname,
      email: user.email,
      phone: user.phone,
      status: user.status,
      roleIds: []
    })
  } else {
    Object.assign(form, {username: '', password: '', nickname: '', email: '', phone: '', roleIds: [], status: 1})
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!editingUser.value) {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
  }
  submitting.value = true
  try {
    if (editingUser.value) {
      await updateUser(editingUser.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createUser(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  await deleteUser(id)
  ElMessage.success('删除成功')
  loadUsers()
}

onMounted(() => {
  loadUsers();
  loadRoles()
})
</script>

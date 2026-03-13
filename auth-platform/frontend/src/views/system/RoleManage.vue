<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>角色管理</span>
        <el-button type="primary" @click="openDialog(null)">新增角色</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="roles" stripe>
      <el-table-column label="ID" prop="id" width="60"/>
      <el-table-column label="角色名称" prop="roleName"/>
      <el-table-column label="角色标识" prop="roleKey"/>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑角色' : '新增角色'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName"/>
        </el-form-item>
        <el-form-item label="角色标识">
          <el-input v-model="form.roleKey" :disabled="!!editing"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark"/>
        </el-form-item>
        <el-form-item label="权限">
          <el-tree ref="treeRef" :data="permTree" :default-checked-keys="form.permissionIds" :props="{ label: 'permissionName', children: 'children' }"
                   node-key="id"
                   show-checkbox/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {getRoles, createRole, updateRole, deleteRole, getRolePermissions} from '../../api/role'
import {getPermissionTree} from '../../api/permission'
import {ElMessage} from 'element-plus'

const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const roles = ref([])
const permTree = ref([])
const treeRef = ref(null)
const form = reactive({roleName: '', roleKey: '', remark: '', permissionIds: []})

const loadRoles = async () => {
  loading.value = true
  try {
    const res = await getRoles({pageNum: 1, pageSize: 100});
    roles.value = res.data.records
  } finally {
    loading.value = false
  }
}

const loadPermTree = async () => {
  const res = await getPermissionTree();
  permTree.value = res.data
}

const openDialog = async (role) => {
  editing.value = role
  if (role) {
    Object.assign(form, {roleName: role.roleName, roleKey: role.roleKey, remark: role.remark})
    const res = await getRolePermissions(role.id)
    form.permissionIds = res.data
  } else {
    Object.assign(form, {roleName: '', roleKey: '', remark: '', permissionIds: []})
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  const halfKeys = treeRef.value?.getHalfCheckedKeys() || []
  const data = {...form, permissionIds: [...checkedKeys, ...halfKeys]}
  if (editing.value) {
    await updateRole(editing.value.id, data);
    ElMessage.success('更新成功')
  } else {
    await createRole(data);
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false;
  loadRoles()
}

const handleDelete = async (id) => {
  await deleteRole(id);
  ElMessage.success('删除成功');
  loadRoles()
}

onMounted(() => {
  loadRoles();
  loadPermTree()
})
</script>

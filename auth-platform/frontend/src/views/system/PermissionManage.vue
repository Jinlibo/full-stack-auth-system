<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>权限管理</span>
        <el-button type="primary" @click="openDialog(null)">新增权限</el-button>
      </div>
    </template>
    <el-table :data="permTree" :tree-props="{ children: 'children' }" row-key="id" stripe>
      <el-table-column label="权限名称" prop="permissionName"/>
      <el-table-column label="权限标识" prop="permissionKey"/>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? '' : row.type === 2 ? 'warning' : 'info'" size="small">
            {{ row.type === 1 ? '菜单' : row.type === 2 ? '按钮' : 'API' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="路径" prop="path"/>
      <el-table-column label="排序" prop="sortOrder" width="60"/>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑权限' : '新增权限'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="权限名称">
          <el-input v-model="form.permissionName"/>
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permissionKey"/>
        </el-form-item>
        <el-form-item label="父权限">
          <el-tree-select v-model="form.parentId" :data="parentOptions"
                          :props="{ label: 'permissionName', value: 'id', children: 'children' }"
                          check-strictly clearable placeholder="无(顶级)" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
            <el-radio :value="3">API</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="form.path"/>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0"/>
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
import {ref, reactive, computed, onMounted} from 'vue'
import {getPermissionTree, createPermission, updatePermission, deletePermission} from '../../api/permission'
import {ElMessage} from 'element-plus'

const dialogVisible = ref(false)
const editing = ref(null)
const permTree = ref([])
const form = reactive({permissionName: '', permissionKey: '', parentId: 0, type: 1, path: '', sortOrder: 0})
const parentOptions = computed(() => [{id: 0, permissionName: '无(顶级)', children: permTree.value}])

const load = async () => {
  const res = await getPermissionTree();
  permTree.value = res.data
}

const openDialog = (perm) => {
  editing.value = perm
  if (perm) Object.assign(form, perm)
  else Object.assign(form, {permissionName: '', permissionKey: '', parentId: 0, type: 1, path: '', sortOrder: 0})
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (editing.value) {
    await updatePermission(editing.value.id, form);
    ElMessage.success('更新成功')
  } else {
    await createPermission(form);
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false;
  load()
}

const handleDelete = async (id) => {
  await deletePermission(id);
  ElMessage.success('删除成功');
  load()
}

onMounted(load)
</script>

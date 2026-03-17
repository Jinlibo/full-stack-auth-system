<template>
  <!-- ============================================================
       权限管理页（PermissionManage）
       功能：权限列表、新增/编辑/删除权限
       ============================================================ -->
  <div class="page-container">

    <!-- ——— 页头：标题 + 新增按钮 ——— -->
    <div class="page-header">
      <h2 class="page-title">权限管理</h2>
      <el-button type="primary" class="add-btn" @click="openAddDialog">
        <el-icon><Plus/></el-icon>
        新增权限
      </el-button>
    </div>

    <!-- ——— 权限列表表格 ——— -->
    <div class="table-card">
      <el-table
          v-loading="tableLoading"
          :data="tableData"
          stripe
          style="width: 100%"
      >
        <el-table-column prop="permissionName" label="权限名称" min-width="140"/>
        <el-table-column prop="permissionKey" label="权限标识" min-width="160"/>
        <el-table-column prop="type" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type]?.tagType || ''" size="small">
              {{ typeTagMap[row.type]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" min-width="160" show-overflow-tooltip/>
        <el-table-column prop="sort" label="排序" width="70" align="center"/>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ============================================================
         新增 / 编辑权限弹窗
         ============================================================ -->
    <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑权限' : '新增权限'"
        width="500px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form
          ref="permFormRef"
          :model="permForm"
          :rules="permRules"
          label-width="90px"
      >
        <el-form-item label="权限名称" prop="permissionName">
          <el-input v-model="permForm.permissionName" placeholder="请输入权限名称" clearable/>
        </el-form-item>
        <el-form-item label="权限标识" prop="permissionKey">
          <el-input v-model="permForm.permissionKey" placeholder="如：user:list、user:create" clearable/>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="permForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="菜单" :value="1"/>
            <el-option label="按钮" :value="2"/>
            <el-option label="API" :value="3"/>
          </el-select>
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="permForm.path" placeholder="如：/users（可选）" clearable/>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="permForm.sort" :min="0" :max="9999" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
              v-model="permForm.statusBool"
              active-text="启用"
              inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
/**
 * 权限管理页 - 脚本逻辑
 *
 * 功能：
 * - 加载所有权限列表
 * - 新增 / 编辑 / 删除权限
 */
import {ref, reactive, onMounted} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {listPermissions, createPermission, updatePermission, deletePermission} from '../../api/permission'

/* ——— 类型映射：数字 → 显示文字和 Tag 颜色 ——— */
const typeTagMap = {
  1: {label: '菜单', tagType: ''},
  2: {label: '按钮', tagType: 'warning'},
  3: {label: 'API', tagType: 'info'},
}

/* ——— 表格数据 ——— */
const tableLoading = ref(false)
const tableData = ref([])

/* ——— 新增/编辑弹窗 ——— */
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const permFormRef = ref(null)
const permForm = reactive({
  id: null,
  permissionName: '',
  permissionKey: '',
  type: 1,
  path: '',
  sort: 0,
  statusBool: true,
})

const permRules = {
  permissionName: [{required: true, message: '请输入权限名称', trigger: 'blur'}],
  permissionKey: [{required: true, message: '请输入权限标识', trigger: 'blur'}],
  type: [{required: true, message: '请选择类型', trigger: 'change'}],
}

/**
 * 加载所有权限
 */
const loadPermissions = async () => {
  tableLoading.value = true
  try {
    const res = await listPermissions()
    tableData.value = res.data || []
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    tableLoading.value = false
  }
}

/**
 * 打开新增弹窗
 */
const openAddDialog = () => {
  isEdit.value = false
  Object.assign(permForm, {
    id: null,
    permissionName: '',
    permissionKey: '',
    type: 1,
    path: '',
    sort: 0,
    statusBool: true,
  })
  dialogVisible.value = true
}

/**
 * 打开编辑弹窗，回填数据
 */
const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(permForm, {
    id: row.id,
    permissionName: row.permissionName,
    permissionKey: row.permissionKey,
    type: row.type,
    path: row.path || '',
    sort: row.sort || 0,
    statusBool: row.status === 1,
  })
  dialogVisible.value = true
}

/**
 * 提交新增或编辑
 */
const handleSubmit = async () => {
  const valid = await permFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload = {
      permissionName: permForm.permissionName,
      permissionKey: permForm.permissionKey,
      type: permForm.type,
      path: permForm.path,
      sort: permForm.sort,
      status: permForm.statusBool ? 1 : 0,
    }
    if (isEdit.value) {
      await updatePermission(permForm.id, payload)
      ElMessage.success('权限更新成功')
    } else {
      await createPermission(payload)
      ElMessage.success('权限创建成功')
    }
    dialogVisible.value = false
    loadPermissions()
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    submitLoading.value = false
  }
}

/**
 * 删除权限（二次确认）
 */
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定要删除权限「${row.permissionName}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
  }).catch(() => null)

  try {
    await deletePermission(row.id)
    ElMessage.success('删除成功')
    loadPermissions()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

onMounted(loadPermissions)
</script>

<style scoped>
/* 页面容器 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 页头：标题 + 按钮 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

/* 新增按钮：青绿渐变 */
.add-btn {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  border: none;
}

.add-btn:hover {
  opacity: 0.9;
}

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
</style>

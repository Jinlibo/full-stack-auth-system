<template>
  <!-- ============================================================
       角色管理页（RoleManage）
       功能：角色列表、新增/编辑/删除角色、权限分配
       ============================================================ -->
  <div class="page-container">

    <!-- ——— 页头：标题 + 新增按钮 ——— -->
    <div class="page-header">
      <h2 class="page-title">角色管理</h2>
      <el-button type="primary" class="add-btn" @click="openAddDialog">
        <el-icon><Plus/></el-icon>
        新增角色
      </el-button>
    </div>

    <!-- ——— 角色列表表格 ——— -->
    <div class="table-card">
      <el-table
          v-loading="tableLoading"
          :data="tableData"
          stripe
          style="width: 100%"
      >
        <el-table-column prop="roleName" label="角色名称" min-width="140"/>
        <el-table-column prop="roleKey" label="角色标识" min-width="140"/>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip/>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="openPermDialog(row)">权限分配</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadRoles"
            @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- ============================================================
         新增 / 编辑角色弹窗
         ============================================================ -->
    <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑角色' : '新增角色'"
        width="480px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form
          ref="roleFormRef"
          :model="roleForm"
          :rules="roleRules"
          label-width="90px"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" clearable/>
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="roleForm.roleKey" placeholder="如：ADMIN、USER" clearable/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
              v-model="roleForm.remark"
              type="textarea"
              :rows="3"
              placeholder="角色备注（可选）"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
              v-model="roleForm.statusBool"
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

    <!-- ============================================================
         权限分配弹窗
         ============================================================ -->
    <el-dialog
        v-model="permDialogVisible"
        title="权限分配"
        width="560px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <div v-loading="permLoading" class="perm-dialog-body">
        <p class="perm-tip">为角色 <strong>{{ currentRole?.roleName }}</strong> 分配权限：</p>
        <div class="perm-list">
          <el-checkbox-group v-model="selectedPermIds">
            <div
                v-for="perm in allPermissions"
                :key="perm.id"
                class="perm-item"
            >
              <el-checkbox :label="perm.id">
                <span class="perm-name">{{ perm.permissionName }}</span>
                <span class="perm-key">{{ perm.permissionKey }}</span>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <el-empty v-if="allPermissions.length === 0" description="暂无权限数据"/>
        </div>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="permSubmitLoading" @click="handlePermSubmit">保 存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
/**
 * 角色管理页 - 脚本逻辑
 *
 * 功能：
 * - 分页查询角色列表
 * - 新增 / 编辑 / 删除角色
 * - 权限分配（加载所有权限 + 当前角色已有权限 → 多选 → 保存）
 */
import {ref, reactive, onMounted} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {pageRoles, createRole, updateRole, deleteRole, getRolePermissions, assignRolePermissions} from '../../api/role'
import {listPermissions} from '../../api/permission'

/* ——— 表格数据 ——— */
const tableLoading = ref(false)
const tableData = ref([])

/* ——— 分页状态 ——— */
const pagination = reactive({pageNum: 1, pageSize: 10, total: 0})

/* ——— 新增/编辑弹窗 ——— */
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const roleFormRef = ref(null)
const roleForm = reactive({id: null, roleName: '', roleKey: '', remark: '', statusBool: true})

const roleRules = {
  roleName: [{required: true, message: '请输入角色名称', trigger: 'blur'}],
  roleKey: [{required: true, message: '请输入角色标识', trigger: 'blur'}],
}

/* ——— 权限分配弹窗 ——— */
const permDialogVisible = ref(false)
const permLoading = ref(false)
const permSubmitLoading = ref(false)
const currentRole = ref(null)
const allPermissions = ref([])
const selectedPermIds = ref([])

/**
 * 加载角色分页列表
 */
const loadRoles = async () => {
  tableLoading.value = true
  try {
    const res = await pageRoles({pageNum: pagination.pageNum, pageSize: pagination.pageSize})
    tableData.value = res.data?.records || res.data?.list || res.data || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    tableLoading.value = false
  }
}

/** 切换每页条数 */
const handleSizeChange = () => {
  pagination.pageNum = 1
  loadRoles()
}

/**
 * 打开新增弹窗
 */
const openAddDialog = () => {
  isEdit.value = false
  Object.assign(roleForm, {id: null, roleName: '', roleKey: '', remark: '', statusBool: true})
  dialogVisible.value = true
}

/**
 * 打开编辑弹窗，回填数据
 */
const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(roleForm, {
    id: row.id,
    roleName: row.roleName,
    roleKey: row.roleKey,
    remark: row.remark || '',
    statusBool: row.status === 1,
  })
  dialogVisible.value = true
}

/**
 * 提交新增或编辑
 */
const handleSubmit = async () => {
  const valid = await roleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload = {
      roleName: roleForm.roleName,
      roleKey: roleForm.roleKey,
      remark: roleForm.remark,
      status: roleForm.statusBool ? 1 : 0,
    }
    if (isEdit.value) {
      await updateRole(roleForm.id, payload)
      ElMessage.success('角色更新成功')
    } else {
      await createRole(payload)
      ElMessage.success('角色创建成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    submitLoading.value = false
  }
}

/**
 * 删除角色（二次确认）
 */
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定要删除角色「${row.roleName}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
  }).catch(() => null)

  try {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

/**
 * 打开权限分配弹窗
 * 1. 加载所有权限列表
 * 2. 加载当前角色已分配的权限 ID 列表
 */
const openPermDialog = async (row) => {
  currentRole.value = row
  permDialogVisible.value = true
  permLoading.value = true
  selectedPermIds.value = []
  try {
    const [permRes, rolePermRes] = await Promise.all([
      listPermissions(),
      getRolePermissions(row.id),
    ])
    allPermissions.value = permRes.data || []
    selectedPermIds.value = rolePermRes.data || []
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    permLoading.value = false
  }
}

/**
 * 保存权限分配
 */
const handlePermSubmit = async () => {
  permSubmitLoading.value = true
  try {
    await assignRolePermissions(currentRole.value.id, selectedPermIds.value)
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    permSubmitLoading.value = false
  }
}

onMounted(loadRoles)
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

/* 分页区域 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 权限弹窗内容区 */
.perm-dialog-body {
  min-height: 120px;
}

.perm-tip {
  font-size: 14px;
  color: #606266;
  margin: 0 0 12px;
}

/* 权限列表：滚动区域 */
.perm-list {
  max-height: 360px;
  overflow-y: auto;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  padding: 8px 12px;
}

/* 每个权限选项行 */
.perm-item {
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
}

.perm-item:last-child {
  border-bottom: none;
}

.perm-name {
  font-size: 13px;
  color: #303133;
  margin-right: 8px;
}

.perm-key {
  font-size: 12px;
  color: #909399;
}
</style>

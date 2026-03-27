<template>
  <!-- ============================================================
       角色管理页面
       功能：展示角色列表、新增/编辑角色（含权限树分配）、删除角色
       ============================================================ -->
  <div class="page-container">

    <!-- ——— 页面头部 ——— -->
    <div class="page-header">
      <div class="page-header-icon">
        <el-icon :size="22">
          <UserFilled/>
        </el-icon>
      </div>
      <div class="page-header-text">
        <h2 class="page-title">角色管理</h2>
        <p class="page-desc">定义系统角色并为其分配权限，实现 RBAC 访问控制</p>
      </div>
    </div>

    <!-- ——— 主内容卡片 ——— -->
    <div class="content-card">

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <!-- 当前共有多少条角色记录 -->
          <span class="record-count">共 <b>{{ roles.length }}</b> 个角色</span>
        </div>
        <div class="toolbar-right">
          <!-- 新增角色按钮 -->
          <el-button type="primary" @click="openDialog(null)">
            <el-icon>
              <Plus/>
            </el-icon>
            新增角色
          </el-button>
        </div>
      </div>

      <!-- ——— 角色数据表格 ——— -->
      <el-table
          v-loading="loading"
          :data="roles"
          class="data-table"
          stripe
      >
        <!-- 角色 ID -->
        <el-table-column align="center" label="ID" prop="id" width="65">
          <template #default="{ row }">
            <span class="id-badge">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <!-- 角色名称 -->
        <el-table-column label="角色名称" min-width="140" prop="roleName">
          <template #default="{ row }">
            <div class="role-name-cell">
              <div class="role-dot"></div>
              <span>{{ row.roleName }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 角色标识：通常用于 @PreAuthorize 注解中的权限标识 -->
        <el-table-column label="角色标识" min-width="160" prop="roleKey">
          <template #default="{ row }">
            <el-tag class="key-tag" effect="plain" size="small" type="info">
              {{ row.roleKey }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 备注说明 -->
        <el-table-column label="备注" min-width="180" prop="remark" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="remark-text">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column align="center" label="状态" width="90">
          <template #default="{ row }">
            <el-tag
                :type="row.status === 1 ? 'success' : 'danger'"
                effect="light"
                size="small"
            >
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 操作列：编辑 + 删除 -->
        <el-table-column align="center" label="操作" width="170">
          <template #default="{ row }">
            <div style="display: flex; justify-content: center; align-items: center; gap: 8px;">
              <!-- 编辑：加载权限树并回填当前角色已有的权限 -->
              <el-button plain size="small" type="primary" @click="openDialog(row)" style="margin: 0;">
                <el-icon><Edit/></el-icon>编辑
              </el-button>
              <el-popconfirm
                  title="确认删除该角色?"
                  width="180"
                  @confirm="handleDelete(row.id)"
              >
                <template #reference>
                  <el-button plain size="small" type="danger" style="margin: 0;">
                    <el-icon><Delete/></el-icon>删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ——— 新增 / 编辑角色对话框 ——— -->
    <el-dialog
        v-model="dialogVisible"
        :title="editing ? '编辑角色' : '新增角色'"
        class="form-dialog"
        destroy-on-close
        width="620px"
    >
      <el-form :model="form" class="dialog-form" label-width="90px">

        <!-- 角色名称 -->
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" placeholder="如：系统管理员"/>
        </el-form-item>

        <!-- 角色标识：编辑时禁止修改，防止权限引用错乱 -->
        <el-form-item label="角色标识">
          <el-input
              v-model="form.roleKey"
              :disabled="!!editing"
              placeholder="如：ADMIN（创建后不可修改）"
          />
        </el-form-item>

        <!-- 备注 -->
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="角色说明（选填）"/>
        </el-form-item>

        <!-- 权限分配：树形复选框，支持父子联动 -->
        <el-form-item label="权限分配">
          <div class="perm-tree-wrap">
            <el-tree
                ref="treeRef"
                :data="permTree"
                :default-checked-keys="form.permissionIds"
                :props="{ label: 'permissionName', children: 'children' }"
                class="perm-tree"
                default-expand-all
                node-key="id"
                show-checkbox
            />
          </div>
        </el-form-item>
      </el-form>

      <!-- 对话框底部按钮 -->
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button :loading="submitting" type="primary" @click="handleSubmit">
            确 定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 角色管理页面 - 脚本逻辑
 *
 * 核心流程：
 *  1. 页面加载时拉取角色列表和权限树
 *  2. 点击"编辑"时额外请求该角色已绑定的权限 ID 列表，预先勾选
 *  3. 提交时收集 el-tree 中全选中（checked）和半选中（half-checked）的节点 ID
 *     合并后传给后端，实现父子权限的完整保存
 */
import {ref, reactive, onMounted} from 'vue'
import {getRoles, createRole, updateRole, deleteRole, getRolePermissions} from '../../api/role'
import {getPermissionTree} from '../../api/permission'
import {ElMessage} from 'element-plus'

// ——— 响应式状态 ———

/** 表格加载状态 */
const loading = ref(false)

/** 对话框提交加载状态，防止重复点击 */
const submitting = ref(false)

/** 控制对话框显示/隐藏 */
const dialogVisible = ref(false)

/** 当前正在编辑的角色；null 表示新增模式 */
const editing = ref(null)

/** 角色列表数据 */
const roles = ref([])

/** 权限树数据，用于对话框内的 el-tree 展示 */
const permTree = ref([])

/** el-tree 组件引用，用于调用 getCheckedKeys() 等方法 */
const treeRef = ref(null)

/**
 * 对话框表单数据
 * - roleName:       角色名称
 * - roleKey:        角色标识（唯一标识，创建后不允许修改）
 * - remark:         备注说明
 * - permissionIds:  该角色已绑定的权限 ID 列表（用于 el-tree 默认勾选）
 */
const form = reactive({
  roleName: '',
  roleKey: '',
  remark: '',
  permissionIds: [],
})

// ——— 数据加载 ———

/**
 * 加载角色列表（不分页，pageSize=100 获取全量）
 */
const loadRoles = async () => {
  loading.value = true
  try {
    const res = await getRoles({pageNum: 1, pageSize: 100})
    // res.data.records 是 MyBatis-Plus IPage 中的数据列表
    roles.value = res.data.records
  } finally {
    loading.value = false
  }
}

/**
 * 加载权限树
 * 后端返回树形结构（递归嵌套），每个节点包含 children 子节点列表
 */
const loadPermTree = async () => {
  const res = await getPermissionTree()
  permTree.value = res.data
}

// ——— 对话框操作 ———

/**
 * 打开新增/编辑对话框
 * @param {Object|null} role - null=新增；传入角色对象=编辑
 */
const openDialog = async (role) => {
  editing.value = role
  if (role) {
    // 编辑模式：回填基础字段
    Object.assign(form, {
      roleName: role.roleName,
      roleKey: role.roleKey,
      remark: role.remark,
    })
    // 请求该角色已绑定的权限 ID 列表，用于 el-tree 预勾选
    const res = await getRolePermissions(role.id)
    form.permissionIds = res.data
  } else {
    // 新增模式：清空所有字段
    Object.assign(form, {roleName: '', roleKey: '', remark: '', permissionIds: []})
  }
  dialogVisible.value = true
}

/**
 * 提交角色表单（新增或编辑）
 *
 * 权限收集逻辑：
 *  el-tree 的 getCheckedKeys() 只返回叶子节点或完整勾选的节点
 *  getHalfCheckedKeys() 返回父节点（部分子节点选中时处于半选状态）
 *  两者合并才能完整保存整棵权限树的勾选关系
 */
const handleSubmit = async () => {
  // 获取完全选中的权限节点 ID
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  // 获取半选中的父节点 ID（父节点有部分子权限被选中）
  const halfKeys = treeRef.value?.getHalfCheckedKeys() || []

  // 合并所有需要保存的权限 ID
  const data = {...form, permissionIds: [...checkedKeys, ...halfKeys]}

  submitting.value = true
  try {
    if (editing.value) {
      // 更新角色：PUT /api/roles/{id}
      await updateRole(editing.value.id, data)
      ElMessage.success('角色更新成功')
    } else {
      // 创建角色：POST /api/roles
      await createRole(data)
      ElMessage.success('角色创建成功')
    }
    dialogVisible.value = false
    loadRoles()
  } finally {
    submitting.value = false
  }
}

/**
 * 删除角色
 * @param {number} id - 要删除的角色 ID
 */
const handleDelete = async (id) => {
  await deleteRole(id)
  ElMessage.success('角色已删除')
  loadRoles()
}

// ——— 生命周期 ———

/** 组件挂载时并行加载角色列表和权限树 */
onMounted(() => {
  loadRoles()
  loadPermTree()
})
</script>

<style scoped>
/* ===================================================================
   页面容器
   =================================================================== */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===================================================================
   页面头部
   =================================================================== */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  /* 角色管理使用绿色渐变，与用户管理的蓝紫色区分 */
  background: linear-gradient(135deg, #56ab2f, #a8e063);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(86, 171, 47, 0.35);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* ===================================================================
   内容卡片
   =================================================================== */
.content-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef0f6;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

/* ===================================================================
   工具栏
   =================================================================== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
  background: #fafafa;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

/* 记录数提示文字 */
.record-count {
  font-size: 13px;
  color: #606266;
}

.record-count b {
  color: #409eff;
}

/* ===================================================================
   数据表格
   =================================================================== */
.data-table {
  width: 100%;
}

.data-table :deep(.el-table__row:hover > td) {
  background-color: #f5fff5 !important;
}

/* ID 徽章 */
.id-badge {
  font-size: 12px;
  color: #c0c4cc;
  font-family: monospace;
}

/* 角色名称单元格：小圆点 + 名称 */
.role-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 角色标识圆点，使用绿色 */
.role-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: linear-gradient(135deg, #56ab2f, #a8e063);
  flex-shrink: 0;
}

/* 角色标识 tag 样式 */
.key-tag {
  font-family: monospace;
  font-size: 12px;
  border-radius: 4px;
}

/* 备注文字颜色稍淡 */
.remark-text {
  color: #909399;
  font-size: 13px;
}

/* ===================================================================
   对话框
   =================================================================== */
.form-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f5f5f5;
}

.form-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.dialog-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

/* 权限树容器：固定高度 + 滚动条 + 边框 */
.perm-tree-wrap {
  width: 100%;
  max-height: 280px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px 12px;
  background: #fafafa;
}

/* 权限树整体样式 */
.perm-tree {
  background: transparent;
}

.perm-tree :deep(.el-tree-node__label) {
  font-size: 13px;
}

/* 对话框底部按钮：右对齐 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

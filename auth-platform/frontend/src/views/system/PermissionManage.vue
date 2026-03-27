<template>
  <!-- ============================================================
       权限管理页面
       功能：以树形表格展示权限列表、支持新增/编辑/删除权限节点
       权限类型分三类：菜单(1)、按钮(2)、API(3)
       ============================================================ -->
  <div class="page-container">

    <!-- ——— 页面头部 ——— -->
    <div class="page-header">
      <div class="page-header-icon">
        <el-icon :size="22">
          <Key/>
        </el-icon>
      </div>
      <div class="page-header-text">
        <h2 class="page-title">权限管理</h2>
        <p class="page-desc">管理系统权限节点，支持菜单、按钮、API 三种类型</p>
      </div>
    </div>

    <!-- ——— 主内容卡片 ——— -->
    <div class="content-card">

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <!-- 类型图例说明 -->
          <div class="legend-list">
            <div class="legend-item">
              <el-tag effect="plain" size="small">菜单</el-tag>
              <span>导航菜单项</span>
            </div>
            <div class="legend-item">
              <el-tag effect="plain" size="small" type="warning">按钮</el-tag>
              <span>页面操作按钮</span>
            </div>
            <div class="legend-item">
              <el-tag effect="plain" size="small" type="info">API</el-tag>
              <span>接口访问权限</span>
            </div>
          </div>
        </div>
        <div class="toolbar-right">
          <!-- 新增顶级权限按钮 -->
          <el-button type="primary" @click="openDialog(null)">
            <el-icon>
              <Plus/>
            </el-icon>
            新增权限
          </el-button>
        </div>
      </div>

      <!-- ——— 权限树形表格 ——— -->
      <!-- row-key="id" 是树形表格必须配置的唯一标识字段 -->
      <!-- tree-props 指定子节点字段名 -->
      <el-table
          :data="permTree"
          :tree-props="{ children: 'children' }"
          class="data-table tree-table-custom"
          :indent="24"
          row-key="id"
          stripe
      >
        <!-- 权限名称：树形展开节点 -->
        <el-table-column label="权限名称" min-width="200" prop="permissionName">
          <template #default="{ row }">
            <div class="perm-name-cell">
              <!-- 根据权限类型显示不同颜色的小图标 -->
              <span :class="`type-dot--${row.type}`" class="type-dot"></span>
              <span class="perm-name">{{ row.permissionName }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 权限标识：用于后端 @PreAuthorize 注解中的权限校验 -->
        <el-table-column label="权限标识" min-width="200" prop="permissionKey">
          <template #default="{ row }">
            <el-tag class="key-tag" effect="plain" size="small" type="info">
              {{ row.permissionKey }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 权限类型标签 -->
        <el-table-column align="center" label="类型" width="90">
          <template #default="{ row }">
            <el-tag
                :type="row.type === 1 ? '' : row.type === 2 ? 'warning' : 'info'"
                effect="light"
                size="small"
            >
              {{ row.type === 1 ? '菜单' : row.type === 2 ? '按钮' : 'API' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 路径（菜单路由或 API 路径） -->
        <el-table-column label="路径 / URL" min-width="180" prop="path" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="path-text">{{ row.path || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 排序号：数值越小越靠前 -->
        <el-table-column align="center" label="排序" prop="sortOrder" width="70">
          <template #default="{ row }">
            <span class="sort-badge">{{ row.sortOrder }}</span>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column align="center" label="操作" width="170">
          <template #default="{ row }">
            <div style="display: flex; justify-content: center; align-items: center; gap: 8px;">
              <!-- 编辑按钮：将当前权限节点数据回填到表单 -->
              <el-button plain size="small" type="primary" @click="openDialog(row)" style="margin: 0;">
                <el-icon><Edit/></el-icon>编辑
              </el-button>
              <!-- 删除：后端会检查是否存在子权限，有子权限则拒绝删除 -->
              <el-popconfirm
                  title="确认删除? 有子权限时无法删除"
                  width="200"
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

    <!-- ——— 新增 / 编辑权限对话框 ——— -->
    <el-dialog
        v-model="dialogVisible"
        :title="editing ? '编辑权限' : '新增权限'"
        class="form-dialog"
        destroy-on-close
        width="520px"
    >
      <el-form :model="form" class="dialog-form" label-width="90px">

        <!-- 权限名称 -->
        <el-form-item label="权限名称">
          <el-input v-model="form.permissionName" placeholder="如：用户管理"/>
        </el-form-item>

        <!-- 权限标识 -->
        <el-form-item label="权限标识">
          <el-input v-model="form.permissionKey" placeholder="如：system:user:query"/>
        </el-form-item>

        <!-- 父权限选择：使用 el-tree-select 以树形下拉展示 -->
        <el-form-item label="父权限">
          <el-tree-select
              v-model="form.parentId"
              :data="parentOptions"
              :props="{ label: 'permissionName', value: 'id', children: 'children' }"
              check-strictly
              clearable
              placeholder="无（顶级权限）"
              style="width: 100%"
          />
        </el-form-item>

        <!-- 权限类型单选：菜单/按钮/API -->
        <el-form-item label="权限类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">
              <el-tag effect="plain" size="small">菜单</el-tag>
            </el-radio>
            <el-radio :value="2">
              <el-tag effect="plain" size="small" type="warning">按钮</el-tag>
            </el-radio>
            <el-radio :value="3">
              <el-tag effect="plain" size="small" type="info">API</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 路径 -->
        <el-form-item label="路径">
          <el-input v-model="form.path" placeholder="路由路径或接口 URL"/>
        </el-form-item>

        <!-- 排序号 -->
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :max="999" :min="0"/>
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
 * 权限管理页面 - 脚本逻辑
 *
 * 核心说明：
 *  - 权限以树形结构组织，每个节点有 parentId 指向其父节点
 *  - 顶级权限的 parentId 为 0
 *  - 后端会阻止删除含有子权限的节点
 *  - 表格使用 el-table 的 tree-props 展示层级关系
 */
import {ref, reactive, computed, onMounted} from 'vue'
import {
  getPermissionTree,
  createPermission,
  updatePermission,
  deletePermission,
} from '../../api/permission'
import {ElMessage} from 'element-plus'

// ——— 响应式状态 ———

/** 控制对话框显示/隐藏 */
const dialogVisible = ref(false)

/** 提交加载状态，防止重复提交 */
const submitting = ref(false)

/** 当前编辑的权限节点；null 表示新增 */
const editing = ref(null)

/** 权限树数据（递归嵌套结构），绑定到 el-table */
const permTree = ref([])

/**
 * 对话框表单数据
 * - permissionName:  权限名称（用于展示）
 * - permissionKey:   权限标识（用于 @PreAuthorize 校验）
 * - parentId:        父权限 ID（0 表示顶级）
 * - type:            类型：1=菜单，2=按钮，3=API
 * - path:            路由路径或接口 URL
 * - sortOrder:       同级节点中的显示排序
 */
const form = reactive({
  permissionName: '',
  permissionKey: '',
  parentId: 0,
  type: 1,
  path: '',
  sortOrder: 0,
})

/**
 * 父权限下拉选项
 * 在权限树顶部插入一个"无（顶级权限）"根节点，使用户可以选择不挂载父节点
 * computed 属性会随 permTree 数据更新自动刷新
 */
const parentOptions = computed(() => [
  {id: 0, permissionName: '无（顶级权限）', children: permTree.value},
])

// ——— 数据加载 ———

/**
 * 加载权限树数据
 * 后端按 sortOrder 排序后构建树形结构返回
 */
const load = async () => {
  const res = await getPermissionTree()
  permTree.value = res.data
}

// ——— 对话框操作 ———

/**
 * 打开对话框
 * @param {Object|null} perm - null=新增；传入权限节点对象=编辑
 */
const openDialog = (perm) => {
  editing.value = perm
  if (perm) {
    // 编辑：直接用 Object.assign 将节点数据复制到表单
    // perm 对象的字段名与 form 一一对应，可以直接赋值
    Object.assign(form, perm)
  } else {
    // 新增：重置所有字段为默认值
    Object.assign(form, {
      permissionName: '',
      permissionKey: '',
      parentId: 0,
      type: 1,
      path: '',
      sortOrder: 0,
    })
  }
  dialogVisible.value = true
}

/**
 * 提交权限表单（新增或编辑）
 */
const handleSubmit = async () => {
  submitting.value = true
  try {
    if (editing.value) {
      // 更新：PUT /api/permissions/{id}
      await updatePermission(editing.value.id, form)
      ElMessage.success('权限更新成功')
    } else {
      // 创建：POST /api/permissions
      await createPermission(form)
      ElMessage.success('权限创建成功')
    }
    dialogVisible.value = false
    // 重新加载权限树以展示最新结构
    load()
  } finally {
    submitting.value = false
  }
}

/**
 * 删除权限节点
 * @param {number} id - 权限 ID
 * 注意：若该节点存在子权限，后端会返回错误，前端通过全局错误拦截器弹出提示
 */
const handleDelete = async (id) => {
  await deletePermission(id)
  ElMessage.success('权限已删除')
  load()
}

// ——— 生命周期 ———

/** 挂载时加载权限树 */
onMounted(load)
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
  /* 权限管理使用红橙渐变，语义上与"安全/钥匙"相关 */
  background: linear-gradient(135deg, #f093fb, #f5576c);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(245, 87, 108, 0.35);
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

/* 图例列表：横向排列 */
.legend-list {
  display: flex;
  gap: 20px;
}

/* 单个图例：tag + 说明文字 */
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

/* ===================================================================
   数据表格
   =================================================================== */
.data-table {
  width: 100%;
}

/* 权限名称单元格：圆点 + 名称 */
.perm-name-cell {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
  gap: 8px;
}

/* 类型圆点：根据 type 类型应用不同颜色 */
.type-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 菜单类型：蓝色 */
.type-dot--1 {
  background: #409eff;
}

/* 按钮类型：橙色 */
.type-dot--2 {
  background: #e6a23c;
}

/* API 类型：灰色 */
.type-dot--3 {
  background: #909399;
}

/* 权限名称文字 */
.perm-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

/* 权限标识：等宽字体，方便阅读 */
.key-tag {
  font-family: monospace;
  font-size: 12px;
}

/* 路径文字：稍浅颜色 */
.path-text {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

/* 排序号徽章：圆形数字 */
.sort-badge {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background: #f4f6fb;
  font-size: 12px;
  color: #606266;
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

/* el-radio 之间增加间距 */
.dialog-form :deep(.el-radio) {
  margin-right: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 树形表格层级辨识度优化 */
.tree-table-custom :deep(.el-table__row--level-1) {
  background-color: #fafbfc !important;
}

.tree-table-custom :deep(.el-table__row--level-2) {
  background-color: #f7f9fb !important;
}

.tree-table-custom :deep(.el-table__placeholder) {
  display: inline-block;
  vertical-align: middle;
}

.tree-table-custom :deep(.el-table__expand-icon) {
  color: #409eff;
  font-weight: bold;
  vertical-align: middle;
  margin-right: 6px;
}
</style>

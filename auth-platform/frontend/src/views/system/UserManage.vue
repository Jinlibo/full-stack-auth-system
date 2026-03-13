<template>
  <!-- ============================================================
       用户管理页面
       功能：分页查询用户列表、关键字搜索、新增/编辑/删除用户、分配角色
       ============================================================ -->
  <div class="page-container">

    <!-- ——— 页面头部：图标 + 标题 + 描述 ——— -->
    <div class="page-header">
      <div class="page-header-icon">
        <!-- 用户图标 SVG -->
        <el-icon :size="22">
          <User/>
        </el-icon>
      </div>
      <div class="page-header-text">
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理系统中的所有用户账号，支持角色分配与状态控制</p>
      </div>
    </div>

    <!-- ——— 主内容卡片 ——— -->
    <div class="content-card">

      <!-- 工具栏：左侧搜索框 + 右侧新增按钮 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <!-- 关键字搜索框：支持按用户名、昵称、邮箱模糊搜索 -->
          <el-input
              v-model="query.keyword"
              class="search-input"
              clearable
              placeholder="搜索用户名 / 昵称 / 邮箱"
              @clear="loadUsers"
              @keyup.enter="loadUsers"
          >
            <template #prefix>
              <el-icon>
                <Search/>
              </el-icon>
            </template>
          </el-input>
          <!-- 搜索按钮 -->
          <el-button plain type="primary" @click="loadUsers">
            <el-icon>
              <Search/>
            </el-icon>
            搜索
          </el-button>
        </div>
        <div class="toolbar-right">
          <!-- 新增用户按钮，点击后打开新增对话框 -->
          <el-button type="primary" @click="openDialog(null)">
            <el-icon>
              <Plus/>
            </el-icon>
            新增用户
          </el-button>
        </div>
      </div>

      <!-- ——— 用户数据表格 ——— -->
      <el-table
          v-loading="loading"
          :data="users"
          class="data-table"
          row-class-name="table-row"
          stripe
      >
        <!-- 用户ID列 -->
        <el-table-column align="center" label="ID" prop="id" width="65">
          <template #default="{ row }">
            <span class="id-badge">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <!-- 用户名列 -->
        <el-table-column label="用户名" prop="username" width="130">
          <template #default="{ row }">
            <!-- 头像缩略 + 用户名，增强视觉层次 -->
            <div class="user-cell">
              <el-avatar :size="28" class="mini-avatar">
                {{ row.nickname?.charAt(0)?.toUpperCase() || row.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username-text">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 昵称列 -->
        <el-table-column label="昵称" prop="nickname" width="120"/>

        <!-- 邮箱列 -->
        <el-table-column label="邮箱" min-width="160" prop="email" show-overflow-tooltip/>

        <!-- 手机号列 -->
        <el-table-column label="手机号" prop="phone" width="130"/>

        <!-- 角色列：以标签形式展示用户拥有的所有角色 -->
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag
                  v-for="r in row.roles"
                  :key="r"
                  class="role-tag"
                  effect="plain"
                  size="small"
              >{{ r }}
              </el-tag>
              <!-- 无角色时显示占位文本 -->
              <span v-if="!row.roles?.length" class="no-data-text">暂无角色</span>
            </el-space>
          </template>
        </el-table-column>

        <!-- 状态列：正常=绿色，禁用=红色 -->
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

        <!-- 操作列：编辑 + 删除（带二次确认） -->
        <el-table-column align="center" fixed="right" label="操作" width="150">
          <template #default="{ row }">
            <!-- 编辑按钮：打开编辑对话框并回填当前行数据 -->
            <el-button plain size="small" type="primary" @click="openDialog(row)">
              <el-icon>
                <Edit/>
              </el-icon>
              编辑
            </el-button>
            <!-- 删除按钮：popconfirm 二次确认后再执行删除 -->
            <el-popconfirm
                title="确认删除该用户?"
                width="180"
                @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button plain size="small" type="danger">
                  <el-icon>
                    <Delete/>
                  </el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- ——— 分页器 ——— -->
      <div class="pagination-wrap">
        <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            background
            layout="total, sizes, prev, pager, next, jumper"
            @change="loadUsers"
        />
      </div>
    </div>

    <!-- ——— 新增 / 编辑用户对话框 ——— -->
    <el-dialog
        v-model="dialogVisible"
        :title="editingUser ? '编辑用户' : '新增用户'"
        class="form-dialog"
        destroy-on-close
        width="520px"
    >
      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="dialog-form"
          label-width="90px"
      >
        <!-- 用户名：仅新增时可填，编辑时不允许修改用户名 -->
        <el-form-item v-if="!editingUser" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名"/>
        </el-form-item>

        <!-- 密码：仅新增时必填 -->
        <el-form-item v-if="!editingUser" label="密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入密码" show-password type="password"/>
        </el-form-item>

        <!-- 昵称 -->
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称"/>
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱"/>
        </el-form-item>

        <!-- 手机号 -->
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号"/>
        </el-form-item>

        <!-- 角色选择：多选下拉，从 allRoles 列表动态加载 -->
        <el-form-item label="角色">
          <el-select
              v-model="form.roleIds"
              multiple
              placeholder="请选择角色"
              style="width: 100%"
          >
            <el-option
                v-for="r in allRoles"
                :key="r.id"
                :label="r.roleName"
                :value="r.id"
            />
          </el-select>
        </el-form-item>

        <!-- 账号状态：仅编辑模式显示，使用开关控件 -->
        <el-form-item v-if="editingUser" label="状态">
          <el-switch
              v-model="form.status"
              :active-value="1"
              :inactive-value="0"
              active-text="正常"
              inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <!-- 对话框底部操作按钮 -->
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
 * 用户管理页面 - 脚本逻辑
 *
 * 主要职责：
 *  1. 分页加载用户列表，支持关键字搜索
 *  2. 新增用户（含角色分配）
 *  3. 编辑用户信息（昵称、邮箱、手机、角色、状态）
 *  4. 删除用户（超级管理员不可删除）
 */
import {ref, reactive, onMounted} from 'vue'
import {getUsers, createUser, updateUser, deleteUser} from '../../api/user'
import {getAllRoles} from '../../api/role'
import {ElMessage} from 'element-plus'

// ——— 响应式状态 ———

/** 表格加载状态：true 时显示 loading 遮罩 */
const loading = ref(false)

/** 对话框提交状态：true 时显示确定按钮的加载动画，防止重复提交 */
const submitting = ref(false)

/** 控制新增/编辑对话框的显示与隐藏 */
const dialogVisible = ref(false)

/** 当前正在编辑的用户对象；为 null 表示新增模式 */
const editingUser = ref(null)

/** 用户列表数据，绑定到 el-table 的 :data */
const users = ref([])

/** 总记录数，用于分页器 */
const total = ref(0)

/** 可选角色列表，用于对话框中的角色多选下拉 */
const allRoles = ref([])

/** 表单 ref，用于调用 validate() 进行校验 */
const formRef = ref(null)

/**
 * 查询参数对象
 * - pageNum:   当前页码（从 1 开始）
 * - pageSize:  每页条数
 * - keyword:   关键字搜索（用户名/昵称/邮箱模糊匹配）
 */
const query = reactive({pageNum: 1, pageSize: 10, keyword: ''})

/**
 * 对话框表单数据
 * - username:  用户名（新增时必填）
 * - password:  密码（新增时必填）
 * - nickname:  昵称
 * - email:     邮箱
 * - phone:     手机号
 * - roleIds:   选中的角色 ID 列表
 * - status:    账号状态 1=正常 0=禁用
 */
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleIds: [],
  status: 1,
})

/**
 * 表单校验规则
 * - 新增时 username 和 password 为必填项
 * - 编辑时两个字段不显示，无需校验
 */
const rules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
}

// ——— 数据加载 ———

/**
 * 分页加载用户列表
 * 将 query 对象作为请求参数传给后端，后端支持：
 *  - pageNum / pageSize：分页
 *  - keyword：模糊搜索（用户名 OR 昵称 OR 邮箱）
 */
const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getUsers(query)
    // res.data 是 MyBatis-Plus 的 IPage 结构，包含 records（列表）和 total（总数）
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    // 无论成功或失败，都关闭 loading
    loading.value = false
  }
}

/**
 * 加载所有角色（不分页），用于对话框中的角色多选框
 */
const loadRoles = async () => {
  const res = await getAllRoles()
  allRoles.value = res.data
}

// ——— 对话框操作 ———

/**
 * 打开新增/编辑对话框
 * @param {Object|null} user - 为 null 时进入新增模式；传入用户对象时进入编辑模式
 */
const openDialog = (user) => {
  editingUser.value = user
  if (user) {
    // 编辑模式：将当前行数据回填到表单
    // 注意：username 和 password 在编辑时不显示，不需要回填
    Object.assign(form, {
      nickname: user.nickname,
      email: user.email,
      phone: user.phone,
      status: user.status,
      roleIds: [], // 角色多选框不做回填（后端接口不返回 roleIds 数组，仅返回 roleKeys 字符串）
    })
  } else {
    // 新增模式：重置所有字段
    Object.assign(form, {
      username: '',
      password: '',
      nickname: '',
      email: '',
      phone: '',
      roleIds: [],
      status: 1,
    })
  }
  dialogVisible.value = true
}

/**
 * 对话框确定按钮点击处理
 * - 新增：先校验表单，再调用创建接口
 * - 编辑：直接调用更新接口（username/password 字段不存在，无需校验）
 */
const handleSubmit = async () => {
  // 新增模式需要校验 username 和 password 是否填写
  if (!editingUser.value) {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
  }
  submitting.value = true
  try {
    if (editingUser.value) {
      // 编辑：PUT /api/users/{id}
      await updateUser(editingUser.value.id, form)
      ElMessage.success('用户信息已更新')
    } else {
      // 新增：POST /api/users
      await createUser(form)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    // 刷新列表以展示最新数据
    loadUsers()
  } finally {
    submitting.value = false
  }
}

/**
 * 删除用户
 * @param {number} id - 要删除的用户 ID
 * 注意：后端会阻止删除 id=1 的超级管理员
 */
const handleDelete = async (id) => {
  await deleteUser(id)
  ElMessage.success('用户已删除')
  loadUsers()
}

// ——— 生命周期 ———

/** 组件挂载后立即加载用户列表和角色列表 */
onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>

<style scoped>
/* ===================================================================
   页面容器：flex 竖向布局，内部包含页头和内容卡片
   =================================================================== */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===================================================================
   页面头部：渐变图标 + 标题 + 描述
   =================================================================== */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 左侧彩色图标圆角框 */
.page-header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}

/* 页面标题 */
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

/* 页面描述 */
.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* ===================================================================
   内容卡片：白色背景、圆角、阴影
   =================================================================== */
.content-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef0f6;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

/* ===================================================================
   工具栏：左右布局，左侧搜索，右侧操作按钮
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
  gap: 10px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 搜索输入框固定宽度 */
.search-input {
  width: 260px;
}

/* ===================================================================
   数据表格
   =================================================================== */
.data-table {
  width: 100%;
}

/* 表格行 hover 效果（通过全局选择器覆盖） */
.data-table :deep(.el-table__row:hover > td) {
  background-color: #f5f7ff !important;
}

/* ID 徽章样式：小号灰色文字 */
.id-badge {
  font-size: 12px;
  color: #c0c4cc;
  font-family: monospace;
}

/* 用户单元格：头像 + 用户名横向排列 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 迷你头像：渐变背景 */
.mini-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

/* 用户名文字 */
.username-text {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

/* 角色标签样式 */
.role-tag {
  border-radius: 20px;
  font-size: 11px;
}

/* 无数据占位文字 */
.no-data-text {
  color: #c0c4cc;
  font-size: 12px;
}

/* ===================================================================
   分页器包装：右对齐
   =================================================================== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #f5f5f5;
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

/* 对话框底部按钮区域：右对齐 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 4px;
}
</style>

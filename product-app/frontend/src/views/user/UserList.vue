<template>
  <!-- ============================================================
       用户管理页（UserList）
       功能：分页展示产品应用用户列表，支持关键词搜索
       ============================================================ -->
  <div class="page-container">

    <!-- ——— 页头：标题 + 图标 ——— -->
    <div class="page-header">
      <div class="page-header-icon">
        <el-icon :size="22">
          <User/>
        </el-icon>
      </div>
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">管理产品应用的用户账号及 OAuth2 绑定状态</p>
      </div>
    </div>

    <!-- ——— 内容卡片 ——— -->
    <div class="content-card">

      <!-- 工具栏：搜索框 -->
      <div class="toolbar">
        <el-input
            v-model="query.keyword"
            clearable
            placeholder="搜索用户名、昵称、邮箱..."
            prefix-icon="Search"
            style="width: 280px;"
            @clear="load"
            @keyup.enter="load"
        />
        <el-button type="primary" @click="load">
          <el-icon>
            <Search/>
          </el-icon>
          搜 索
        </el-button>
      </div>

      <!-- 用户数据表格 -->
      <el-table
          v-loading="loading"
          :data="users"
          class="data-table"
          row-key="id"
          stripe
      >
        <!-- ID 列：等宽，monospace 字体 -->
        <el-table-column label="ID" prop="id" width="70">
          <template #default="{ row }">
            <span class="id-badge">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <!-- 用户信息列：头像 + 用户名 + 昵称（合并展示） -->
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <!-- 小头像：显示昵称/用户名首字母 -->
              <el-avatar :size="30" class="mini-avatar">
                {{ (row.nickname || row.username)?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div>
                <div class="user-name">{{ row.username }}</div>
                <div v-if="row.nickname" class="user-nickname">{{ row.nickname }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 邮箱 -->
        <el-table-column label="邮箱" min-width="160" prop="email">
          <template #default="{ row }">
            <span style="color: #606266;">{{ row.email || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 角色标签 -->
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag
                  v-for="r in row.roles"
                  :key="r"
                  effect="plain"
                  size="small"
                  type="success"
              >{{ r }}
              </el-tag>
              <span v-if="!row.roles?.length" style="color: #c0c4cc; font-size: 12px;">暂无角色</span>
            </el-space>
          </template>
        </el-table-column>

        <!-- OAuth2 绑定状态 -->
        <el-table-column label="OAuth2 绑定" min-width="140">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag
                  v-for="b in row.oauthBindings"
                  :key="b.provider"
                  effect="plain"
                  size="small"
                  type="primary"
              >{{ b.provider }}
              </el-tag>
              <!-- 未绑定时显示横线 -->
              <span v-if="!row.oauthBindings?.length" style="color: #c0c4cc; font-size: 12px;">-</span>
            </el-space>
          </template>
        </el-table-column>

        <!-- 账号状态 -->
        <el-table-column align="center" label="状态" width="80">
          <template #default="{ row }">
            <el-tag
                :type="row.status === 1 ? 'success' : 'danger'"
                effect="plain"
                size="small"
            >{{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页控件：右对齐 -->
      <div class="pagination-wrap">
        <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            background
            layout="total, sizes, prev, pager, next, jumper"
            @change="load"
        />
      </div>

    </div>
  </div>
</template>

<script setup>
/**
 * 用户管理页 - 脚本逻辑
 *
 * - 分页加载用户列表：getUsers({ pageNum, pageSize, keyword })
 * - 支持关键词实时搜索（回车触发或点击搜索按钮）
 * - 展示用户的角色列表和 OAuth2 绑定的第三方提供商
 */
import {ref, reactive, onMounted} from 'vue'
import {getUsers} from '../../api/user'

/** 表格加载状态，控制 v-loading 骨架屏 */
const loading = ref(false)

/** 用户列表数据（绑定到 el-table :data） */
const users = ref([])

/** 总记录数（用于 el-pagination :total） */
const total = ref(0)

/**
 * 分页查询参数（响应式对象）
 * - pageNum：当前页码（从 1 开始）
 * - pageSize：每页条数
 * - keyword：搜索关键词（空字符串表示不过滤）
 */
const query = reactive({pageNum: 1, pageSize: 10, keyword: ''})

/**
 * 加载用户列表
 * 调用 GET /api/users?pageNum=&pageSize=&keyword=
 * 响应格式：{ data: { records: [], total: n } }
 */
const load = async () => {
  loading.value = true
  try {
    const res = await getUsers(query)
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/** 页面挂载时自动加载第一页数据 */
onMounted(load)
</script>

<style scoped>
/* ===================================================================
   页面容器
   =================================================================== */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ===================================================================
   页头：图标 + 标题 + 副标题
   =================================================================== */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

/* 渐变图标背景 */
.page-header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #11998e, #38ef7d);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(17, 153, 142, 0.3);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.page-subtitle {
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
  border: 1px solid #f0f0f0;
  padding: 20px;
}

/* ===================================================================
   工具栏：搜索框 + 按钮
   =================================================================== */
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

/* ===================================================================
   数据表格自定义样式
   =================================================================== */
.data-table {
  border-radius: 8px;
  overflow: hidden;
}

/* 行 hover 效果 */
.data-table :deep(tr.el-table__row:hover > td) {
  background-color: #edfaf7 !important;
}

/* 表头背景 */
.data-table :deep(th.el-table__cell) {
  background: #fafafa;
  color: #606266;
  font-weight: 600;
  font-size: 13px;
}

/* ID 徽章：monospace 字体，灰色 */
.id-badge {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
}

/* 用户信息组合列：头像 + 用户名 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 小头像：青绿渐变背景 */
.mini-avatar {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.user-nickname {
  font-size: 12px;
  color: #909399;
  margin-top: 1px;
}

/* ===================================================================
   分页控件：右对齐
   =================================================================== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

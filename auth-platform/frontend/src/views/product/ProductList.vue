<template>
  <!-- ============================================================
       产品管理页面
       功能：管理 OAuth2 应用（产品），支持新增、编辑、删除、启用/禁用
       每个产品对应 OAuth2 服务器中的一个 RegisteredClient
       ============================================================ -->
  <div class="page-container">

    <!-- ——— 页面头部 ——— -->
    <div class="page-header">
      <div class="page-header-icon">
        <el-icon :size="22">
          <Grid/>
        </el-icon>
      </div>
      <div class="page-header-text">
        <h2 class="page-title">产品管理</h2>
        <p class="page-desc">管理接入 OAuth2 授权服务的第三方应用，自动生成 Client ID / Secret</p>
      </div>
    </div>

    <!-- ——— 主内容卡片 ——— -->
    <div class="content-card">

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <!-- 关键字搜索：支持产品名称和 Client ID 模糊搜索 -->
          <el-input
              v-model="query.keyword"
              class="search-input"
              clearable
              placeholder="搜索产品名称 / Client ID"
              @clear="load"
              @keyup.enter="load"
          >
            <template #prefix>
              <el-icon>
                <Search/>
              </el-icon>
            </template>
          </el-input>
          <el-button plain type="primary" @click="load">
            <el-icon>
              <Search/>
            </el-icon>
            搜索
          </el-button>
        </div>
        <div class="toolbar-right">
          <!-- 新增产品：后端会自动生成 Client ID 和 Client Secret -->
          <el-button type="primary" @click="openDialog(null)">
            <el-icon>
              <Plus/>
            </el-icon>
            新增产品
          </el-button>
        </div>
      </div>

      <!-- ——— 产品数据表格 ——— -->
      <el-table v-loading="loading" :data="products" class="data-table" stripe>

        <!-- 产品 ID -->
        <el-table-column align="center" label="ID" prop="id" width="65">
          <template #default="{ row }">
            <span class="id-badge">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <!-- 产品名称：带产品图标 -->
        <el-table-column label="产品名称" min-width="150" prop="productName">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="product-icon">
                {{ row.productName?.charAt(0)?.toUpperCase() }}
              </div>
              <span class="product-name">{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- Client ID：OAuth2 注册客户端的唯一标识 -->
        <el-table-column label="Client ID" prop="productKey" width="170">
          <template #default="{ row }">
            <el-tag class="code-tag" effect="plain" size="small" type="info">
              {{ row.productKey }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Client Secret：明文存储在 sys_product 方便展示，OAuth2 服务器存 BCrypt 加密值 -->
        <el-table-column label="Client Secret" width="180">
          <template #default="{ row }">
            <!-- 鼠标悬停 tooltip 显示完整 Secret，页面仅显示前 8 位 + 省略号 -->
            <el-tooltip :content="row.productSecret" :show-after="300" placement="top">
              <div class="secret-cell">
                <el-tag class="code-tag" effect="plain" size="small" type="warning">
                  {{ row.productSecret?.substring(0, 8) }}••••••••
                </el-tag>
                <el-icon class="copy-icon">
                  <CopyDocument/>
                </el-icon>
              </div>
            </el-tooltip>
          </template>
        </el-table-column>

        <!-- 描述 -->
        <el-table-column label="描述" min-width="150" prop="description" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-text">{{ row.description || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 状态开关：点击后直接切换启用/禁用，无需编辑对话框 -->
        <el-table-column align="center" label="状态" width="110">
          <template #default="{ row }">
            <el-switch
                :active-text="row.status === 1 ? '已启用' : ''"
                :inactive-text="row.status !== 1 ? '已禁用' : ''"
                :model-value="row.status === 1"
                @change="handleToggle(row.id)"
            />
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column align="center" label="操作" width="155">
          <template #default="{ row }">
            <!-- 编辑按钮：回填产品信息，注意 Client ID 不可编辑 -->
            <el-button plain size="small" type="primary" @click="openDialog(row)">
              <el-icon>
                <Edit/>
              </el-icon>
              编辑
            </el-button>
            <!-- 删除：同步删除 sys_product 和 oauth2_registered_client 中的记录 -->
            <el-popconfirm
                title="确认删除? 将同步注销 OAuth2 客户端"
                width="220"
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

      <!-- 分页器 -->
      <div class="pagination-wrap">
        <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            background
            layout="total, sizes, prev, pager, next, jumper"
            @change="load"
        />
      </div>
    </div>

    <!-- ——— 新增 / 编辑产品对话框 ——— -->
    <el-dialog
        v-model="dialogVisible"
        :title="editing ? '编辑产品' : '新增产品'"
        class="form-dialog"
        destroy-on-close
        width="580px"
    >
      <el-form :model="form" class="dialog-form" label-width="100px">

        <!-- 产品名称 -->
        <el-form-item label="产品名称">
          <el-input v-model="form.productName" placeholder="请输入产品名称"/>
        </el-form-item>

        <!-- 产品描述 -->
        <el-form-item label="描述">
          <el-input
              v-model="form.description"
              :rows="2"
              placeholder="简短描述产品用途（选填）"
              type="textarea"
          />
        </el-form-item>

        <!-- 产品首页地址：用于 OAuth2 授权页面展示 -->
        <el-form-item label="首页地址">
          <el-input v-model="form.homepageUrl" placeholder="https://your-app.com"/>
        </el-form-item>

        <!-- 回调 URI 列表：支持多个，可动态增删 -->
        <el-form-item label="回调 URI">
          <!-- 循环展示已有 URI，每行一个输入框 + 删除按钮 -->
          <div
              v-for="(uri, i) in form.redirectUris"
              :key="i"
              class="uri-row"
          >
            <el-input
                v-model="form.redirectUris[i]"
                placeholder="https://your-app.com/callback"
            />
            <!-- 删除当前 URI 行 -->
            <el-button
                :icon="'Delete'"
                circle
                plain
                size="small"
                type="danger"
                @click="form.redirectUris.splice(i, 1)"
            />
          </div>
          <!-- 添加新 URI 行按钮 -->
          <el-button size="small" @click="form.redirectUris.push('')">
            <el-icon>
              <Plus/>
            </el-icon>
            添加回调 URI
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 提示信息：新增时显示 Client ID/Secret 自动生成说明 -->
      <el-alert
          v-if="!editing"
          :closable="false"
          class="form-tip"
          show-icon
          type="info"
      >
        <template #default>
          <span>创建后系统将自动生成 <b>Client ID</b> 和 <b>Client Secret</b>，请妥善保存</span>
        </template>
      </el-alert>

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
 * 产品管理页面 - 脚本逻辑
 *
 * 产品（Product）对应 OAuth2 授权服务器中的 RegisteredClient（注册客户端）
 * 新增产品时后端会：
 *  1. 自动生成 Client ID（app-xxxxxxxx 格式）和 Client Secret（UUID）
 *  2. 在 sys_product 表中保存产品信息（含明文 Secret 用于展示）
 *  3. 将 BCrypt 加密后的 Secret 写入 oauth2_registered_client 表
 */
import {ref, reactive, onMounted} from 'vue'
import {
  getProducts,
  createProduct,
  updateProduct,
  deleteProduct,
  toggleProductStatus,
} from '../../api/product'
import {ElMessage} from 'element-plus'

// ——— 响应式状态 ———

/** 表格加载状态 */
const loading = ref(false)

/** 对话框提交加载状态 */
const submitting = ref(false)

/** 控制对话框显示/隐藏 */
const dialogVisible = ref(false)

/** 当前编辑的产品；null 表示新增 */
const editing = ref(null)

/** 产品列表数据 */
const products = ref([])

/** 总记录数 */
const total = ref(0)

/**
 * 查询参数
 * - pageNum:   当前页码
 * - pageSize:  每页条数
 * - keyword:   关键字（产品名称 OR Client ID 模糊搜索）
 */
const query = reactive({pageNum: 1, pageSize: 10, keyword: ''})

/**
 * 对话框表单数据
 * - productName:   产品名称
 * - description:   产品描述
 * - homepageUrl:   首页地址（展示在 OAuth2 授权页上）
 * - redirectUris:  允许的回调 URI 列表（数组，提交时过滤空值）
 */
const form = reactive({
  productName: '',
  description: '',
  homepageUrl: '',
  redirectUris: [''],
})

// ——— 数据加载 ———

/**
 * 分页加载产品列表
 */
const load = async () => {
  loading.value = true
  try {
    const res = await getProducts(query)
    products.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// ——— 对话框操作 ———

/**
 * 打开新增/编辑对话框
 * @param {Object|null} product - null=新增；传入产品对象=编辑
 */
const openDialog = (product) => {
  editing.value = product
  if (product) {
    // 编辑模式：回填产品信息
    // redirectUris 在数据库中以 JSON 字符串存储，需要反序列化
    let uris = []
    try {
      uris = JSON.parse(product.redirectUris || '[]')
    } catch (e) {
      // JSON 解析失败时使用空数组（容错处理）
      uris = []
    }
    Object.assign(form, {
      productName: product.productName,
      description: product.description,
      homepageUrl: product.homepageUrl,
      // 若 URI 列表为空则初始化一个空输入行，保证 UI 始终有输入框
      redirectUris: uris.length ? uris : [''],
    })
  } else {
    // 新增模式：重置所有字段
    Object.assign(form, {
      productName: '',
      description: '',
      homepageUrl: '',
      redirectUris: [''],
    })
  }
  dialogVisible.value = true
}

/**
 * 提交产品表单
 * 提交前过滤掉 redirectUris 中的空字符串
 */
const handleSubmit = async () => {
  // 构造提交数据：过滤用户填写的空 URI 行
  const data = {...form, redirectUris: form.redirectUris.filter((u) => u.trim())}
  submitting.value = true
  try {
    if (editing.value) {
      // 更新：PUT /api/products/{id}
      await updateProduct(editing.value.id, data)
      ElMessage.success('产品信息已更新')
    } else {
      // 创建：POST /api/products（后端自动生成 clientId 和 clientSecret）
      await createProduct(data)
      ElMessage.success('产品创建成功，Client ID / Secret 已自动生成')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

/**
 * 切换产品启用/禁用状态
 * @param {number} id - 产品 ID
 */
const handleToggle = async (id) => {
  await toggleProductStatus(id)
  // 刷新列表以同步最新状态
  load()
}

/**
 * 删除产品
 * @param {number} id - 产品 ID
 * 后端会同步删除 oauth2_registered_client 中对应的客户端记录
 */
const handleDelete = async (id) => {
  await deleteProduct(id)
  ElMessage.success('产品已删除，对应 OAuth2 客户端已注销')
  load()
}

// ——— 生命周期 ———

/** 挂载时加载产品列表 */
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
  /* 产品管理使用橙色渐变，语义上与"产品/应用"相关 */
  background: linear-gradient(135deg, #f7971e, #ffd200);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(247, 151, 30, 0.35);
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
  gap: 10px;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

.search-input {
  width: 280px;
}

/* ===================================================================
   数据表格
   =================================================================== */
.data-table {
  width: 100%;
}

.data-table :deep(.el-table__row:hover > td) {
  background-color: #fffbf0 !important;
}

/* ID 徽章 */
.id-badge {
  font-size: 12px;
  color: #c0c4cc;
  font-family: monospace;
}

/* 产品单元格：小正方形图标 + 产品名称 */
.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 产品名称首字母图标框 */
.product-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f7971e, #ffd200);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.product-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

/* 代码类标签：等宽字体 */
.code-tag {
  font-family: monospace;
  font-size: 12px;
}

/* Secret 单元格：tag + 复制图标 */
.secret-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

/* 复制图标：默认隐藏，hover 时显示 */
.copy-icon {
  font-size: 13px;
  color: #c0c4cc;
  opacity: 0;
  transition: opacity 0.15s;
}

.secret-cell:hover .copy-icon {
  opacity: 1;
  color: #409eff;
}

/* 描述文字 */
.desc-text {
  font-size: 13px;
  color: #909399;
}

/* ===================================================================
   分页器包装
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

/* 回调 URI 输入行：输入框 + 删除按钮横向排列 */
.uri-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  width: 100%;
}

/* 提示信息框 */
.form-tip {
  margin-top: 8px;
  border-radius: 8px;
}

/* 对话框底部按钮：右对齐 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

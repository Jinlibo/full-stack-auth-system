<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>产品管理</span>
        <div>
          <el-input v-model="query.keyword" clearable placeholder="搜索产品" style="width: 200px; margin-right: 10px;"
                    @clear="load" @keyup.enter="load"/>
          <el-button type="primary" @click="openDialog(null)">新增产品</el-button>
        </div>
      </div>
    </template>
    <el-table v-loading="loading" :data="products" stripe>
      <el-table-column label="ID" prop="id" width="60"/>
      <el-table-column label="产品名称" prop="productName"/>
      <el-table-column label="Client ID" prop="productKey" width="160"/>
      <el-table-column label="Client Secret" prop="productSecret" width="160">
        <template #default="{ row }">
          <el-tooltip :content="row.productSecret" placement="top"><span>{{
              row.productSecret?.substring(0, 8)
            }}...</span></el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="描述" prop="description" show-overflow-tooltip/>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="handleToggle(row.id)"/>
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
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                   :total="total" layout="total, sizes, prev, pager, next" style="margin-top: 16px; justify-content: flex-end;"
                   @change="load"/>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑产品' : '新增产品'" width="550px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="产品名称">
          <el-input v-model="form.productName"/>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" :rows="2" type="textarea"/>
        </el-form-item>
        <el-form-item label="首页地址">
          <el-input v-model="form.homepageUrl"/>
        </el-form-item>
        <el-form-item label="回调URI">
          <div v-for="(uri, i) in form.redirectUris" :key="i"
               style="display: flex; gap: 8px; margin-bottom: 8px; width: 100%;">
            <el-input v-model="form.redirectUris[i]"/>
            <el-button :icon="'Delete'" circle size="small" type="danger" @click="form.redirectUris.splice(i, 1)"/>
          </div>
          <el-button size="small" @click="form.redirectUris.push('')">添加回调URI</el-button>
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
import {getProducts, createProduct, updateProduct, deleteProduct, toggleProductStatus} from '../../api/product'
import {ElMessage} from 'element-plus'

const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const products = ref([])
const total = ref(0)
const query = reactive({pageNum: 1, pageSize: 10, keyword: ''})
const form = reactive({productName: '', description: '', homepageUrl: '', redirectUris: ['']})

const load = async () => {
  loading.value = true
  try {
    const res = await getProducts(query);
    products.value = res.data.records;
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const openDialog = (product) => {
  editing.value = product
  if (product) {
    let uris = []
    try {
      uris = JSON.parse(product.redirectUris || '[]')
    } catch (e) {
      uris = []
    }
    Object.assign(form, {
      productName: product.productName,
      description: product.description,
      homepageUrl: product.homepageUrl,
      redirectUris: uris.length ? uris : ['']
    })
  } else {
    Object.assign(form, {productName: '', description: '', homepageUrl: '', redirectUris: ['']})
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const data = {...form, redirectUris: form.redirectUris.filter(u => u)}
  if (editing.value) {
    await updateProduct(editing.value.id, data);
    ElMessage.success('更新成功')
  } else {
    await createProduct(data);
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false;
  load()
}

const handleToggle = async (id) => {
  await toggleProductStatus(id);
  load()
}
const handleDelete = async (id) => {
  await deleteProduct(id);
  ElMessage.success('删除成功');
  load()
}

onMounted(load)
</script>

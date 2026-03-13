<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>用户管理</span>
        <el-input v-model="query.keyword" clearable placeholder="搜索" style="width: 200px;" @clear="load"
                  @keyup.enter="load"/>
      </div>
    </template>
    <el-table v-loading="loading" :data="users" stripe>
      <el-table-column label="ID" prop="id" width="60"/>
      <el-table-column label="用户名" prop="username"/>
      <el-table-column label="昵称" prop="nickname"/>
      <el-table-column label="邮箱" prop="email"/>
      <el-table-column label="角色">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" style="margin: 2px;">{{ r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="OAuth绑定">
        <template #default="{ row }">
          <el-tag v-for="b in row.oauthBindings" :key="b.provider" size="small" type="success">{{ b.provider }}</el-tag>
          <span v-if="!row.oauthBindings?.length" style="color: #999;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{
              row.status === 1 ? '正常' : '禁用'
            }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
                   :total="total" layout="total, sizes, prev, pager, next" style="margin-top: 16px; justify-content: flex-end;"
                   @change="load"/>
  </el-card>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {getUsers} from '../../api/user'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const query = reactive({pageNum: 1, pageSize: 10, keyword: ''})

const load = async () => {
  loading.value = true
  try {
    const res = await getUsers(query);
    users.value = res.data.records;
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

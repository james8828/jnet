<template>
  <div>

    <!-- 查询条件 -->
    <el-form :model="queryParams" label-width="90px" style="margin-bottom: 15px">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="用户名">
            <el-input v-model="queryParams.userName" placeholder="请输入用户名" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="角色">
            <el-select v-model="queryParams.role" placeholder="请选择角色" style="width: 100%">
              <el-option label="管理员" value="admin" />
              <el-option label="普通用户" value="user" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button type="primary" @click="reset">重置</el-button>
          <el-button type="primary" @click="openAddDialog">新增用户</el-button>
        </el-col>
      </el-row>
    </el-form>
    <vxe-grid ref="userGridRef" v-bind="gridOptions">
      <template #action="{ row }">
        <el-button type="warning" @click="openEditDialog(row)">编辑用户</el-button>
      </template>
    </vxe-grid>
  </div>
  <!-- 使用新增组件 -->
  <user-add-dialog ref="addDialogRef" @success="search" />
</template>

<script lang="ts" setup>
import { ref,reactive } from 'vue'
import type { VxeGridInstance, VxeGridProps } from 'vxe-table'
import UserAddDialog from './components/UserAddDialog.vue'
import {getUserInfo,pageUser} from '@api/system/user-api.ts'
const addDialogRef = ref()
const openAddDialog = () => {
  addDialogRef.value.open()
}

const openEditDialog = async (row: RowVO) => {
  const user = await getUserInfo(row.userId)
  addDialogRef.value.open(user)
}
const queryParams = reactive({
  userName: 'james',
  role: ''
})
interface RowVO {
  userId: number
  name: string
  nickname: string
  role: string
  sex: string
  age: number
  address: string
}
const userGridRef = ref<VxeGridInstance<RowVO>>()

const search = () => {
  // 刷新表格并携带查询参数
  // debugger
  userGridRef.value?.commitProxy('query', {page:1 , size:10},queryParams )
}

const reset = () => {
  queryParams.userName = ''
  queryParams.role = ''
  search()
}


const gridOptions = reactive<VxeGridProps<RowVO>>({
  border: true,
  height: 500,
  pagerConfig: {},
  proxyConfig: {
    props: {
      result: 'data.records', // 配置响应结果列表字段
      total: 'data.total' // 配置响应结果总页数字段
    },
    // response: {
    //   total: 'data.total',
    //   result: 'data.records',
    //   message: 'msg'
    // },
    form: true,
    ajax: {
      query: ( g, page, form) => {
        const mergedObj = Object.assign({}, page, form);
        return pageUser(mergedObj)
      }
    }
  },
  columns: [
    { type: 'seq', width: 40 },
    { field: 'userId', title: 'userId', visible: false},
    { field: 'userName', title: '用户名' },
    { field: 'enabled', title: '启用状态' },
    { field: 'sex', title: '性别' },
    { field: 'roleNames', title: '角色' },
    { field: 'createTime', title: '创建时间', showOverflow: true },
    { title: '操作', slots: { default: 'action' } }
  ]
})
</script>
<style scoped>.toolbar {
  margin-bottom: 15px;
}
</style>

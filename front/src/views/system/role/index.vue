<template>
  <div class="table-wrapper">
    <!-- 查询条件 -->
    <el-form :model="queryParams" label-width="100px" style="margin-bottom: 15px">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="角色名称">
            <el-input v-model="queryParams.roleName" placeholder="请输入角色名称"/>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="角色标识">
            <el-input v-model="queryParams.roleKey" placeholder="请输入角色标识"/>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="启用状态">
            <el-select v-model="queryParams.enabled" placeholder="请选择启用状态">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button type="primary" @click="reset">重置</el-button>
          <el-button type="primary" @click="openAddDialog">新增角色</el-button>
        </el-col>
      </el-row>
    </el-form>

    <!-- 表格 -->
    <vxe-grid ref="gridRef" v-bind="gridOptions">
      <template #action="{ row }">
        <el-link type="primary" @click="openEditDialog(row)">编辑</el-link>&nbsp;&nbsp;
        <el-link type="danger" @click="deleteRole(row.roleId)">删除</el-link>
      </template>
    </vxe-grid>

    <!-- 弹窗组件 -->
    <role-form-dialog ref="formDialog" @success="search"/>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import type { VxeGridInstance, VxeGridProps } from 'vxe-table'
import RoleFormDialog from './components/RoleFormDialog.vue'
import {
  addOrUpdateRole,
  deleteRole as apiDeleteRole,
  getRoleById,
  pageRole
} from '@api/system/role-api.ts'
import { Role } from '@types/system.ts'
import {ElMessage} from "element-plus";

// 查询参数
const queryParams = reactive({
  roleName: '',
  roleKey: '',
  enabled: null as boolean | null
})

// 表格引用
const gridRef = ref<VxeGridInstance<Role>>()
const formDialog = ref()

// 搜索方法
const search = () => {
  gridRef.value?.commitProxy('query', {}, queryParams)
}

// 重置方法
const reset = () => {
  queryParams.roleName = ''
  queryParams.roleKey = ''
  queryParams.enabled = null
  search()
}

// 打开新增弹窗
const openAddDialog = () => {
  formDialog.value.open()
}

// 打开编辑弹窗
const openEditDialog = async (row: Role) => {
  const res = await getRoleById(row.roleId)
  if (res.code === 200) {
    formDialog.value.open(res.data)
  }
}

// 删除角色
const deleteRole = async (roleId: number) => {
  try {
    const res = await apiDeleteRole(roleId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      search()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('请求失败')
  }
}

// 表格配置
const gridOptions = reactive<VxeGridProps<Role>>({
  border: true,
  height: 500,
  pagerConfig: {},
  proxyConfig: {
    props: {
      result: 'data.records',
      total: 'data.total'
    },
    form: true,
    ajax: {
      query: (g, page, form) => {
        return pageRole(Object.assign({}, g.page, form))
      }
    }
  },
  columns: [
    { type: 'seq', width: 60, align: 'center' },
    { field: 'roleName', title: '角色名称' },
    { field: 'roleKey', title: '角色标识' },
    { field: 'enabled', title: '启用状态', formatter: ({ cellValue }) => cellValue ? '启用' : '停用' },
    { field: 'createTime', title: '创建时间', showOverflow: true },
    { title: '操作', slots: { default: 'action' }, width: 120, align: 'center' }
  ]
})
</script>

<style scoped>
.table-wrapper {
  margin: 0 15px;
}
.el-space .el-button {
  padding: 8px;
  margin-right: 5px;
}
</style>

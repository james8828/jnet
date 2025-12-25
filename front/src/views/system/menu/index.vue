<template>
  <div class="table-wrapper">
    <!-- 查询条件 -->
    <el-form :model="queryParams" label-width="100px" style="margin-bottom: 15px">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="菜单名称">
            <el-input v-model="queryParams.menuName" placeholder="请输入菜单名称"/>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="路由地址">
            <el-input v-model="queryParams.path" placeholder="请输入路由地址"/>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="权限标识">
            <el-input v-model="queryParams.perms" placeholder="请输入权限标识"/>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button type="primary" @click="reset">重置</el-button>
          <el-button type="primary" @click="openAddDialog">新增菜单</el-button>
        </el-col>
      </el-row>
    </el-form>

    <!-- 表格 -->
    <vxe-grid ref="menuGrid" v-bind="gridOptions">
      <template #action="{ row }">
        <el-link type="primary" @click="openEditDialog(row)">编辑</el-link>&nbsp;&nbsp;
        <el-link type="danger" @click="deleteMenu(row.menuId)">删除</el-link>
      </template>
    </vxe-grid>

    <!-- 弹窗组件 -->
    <menu-form-dialog ref="menuFormDialog" @success="search"/>
  </div>
</template>

<script lang="ts" setup>
import {ref, reactive} from 'vue'
import type {VxeGridInstance, VxeGridProps} from 'vxe-table'
import MenuFormDialog from './components/MenuFormDialog.vue'
import {getMenuInfo, pageMenu, deleteMenu} from '@api/system/menu-api'
import {Menu} from '@types/system.ts'

const menuGrid = ref<VxeGridInstance<Menu>>()
const menuFormDialog = ref()
// 查询参数
const queryParams = reactive({
  menuName: '',
  path: '',
  perms: ''
})



const openAddDialog = () => {
  menuFormDialog.value.open()
}

const openEditDialog = async (row: Menu) => {
  const menu = await getMenuInfo(row.menuId)
  menuFormDialog.value.open(menu.data)
}

const reset = () => {
  queryParams.menuName = ''
  queryParams.path = ''
  queryParams.perms = ''
  search()
}

// 搜索 & 重置
const search = () => {
  menuGrid.value?.commitProxy('query', {}, queryParams)
}

const gridOptions = reactive<VxeGridProps<Menu>>({
  border: true,
  height: '95%',
  pagerConfig: {},
  proxyConfig: {
    props: {
      result: 'data.records', // 配置响应结果列表字段
      total: 'data.total' // 配置响应结果总页数字段
    },
    form: true,
    ajax: {
      query: (g, page, form) => {
        const mergedObj = Object.assign({}, g.page, form);
        return pageMenu(mergedObj)
      }
    }
  },
  columns: [
    {type: 'seq', width: 60, align: 'center'},
    {field: 'menuName', title: '菜单名称'},
    {field: 'parentId', title: '父级ID'},
    {field: 'type', title: '类型'},
    {field: 'path', title: '路由地址'},
    {field: 'component', title: '组件路径'},
    {field: 'visible', title: '是否可见', formatter: ({cellValue}) => cellValue ? '是' : '否'},
    {field: 'enabled', title: '启用状态', formatter: ({cellValue}) => cellValue ? '启用' : '停用'},
    {field: 'perms', title: '权限标识'},
    {field: 'icon', title: '图标'},
    {field: 'createTime', title: '创建时间', showOverflow: true},
    {title: '操作', slots: {default: 'action'}}
  ]
})
</script>
<style scoped>

.table-wrapper {
  margin: 0 15px;
  height: calc(100vh - 120px); /* 根据顶部/底部元素高度调整 */
  overflow: hidden;
}

</style>

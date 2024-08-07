<template>
  <div>
    <vxe-grid v-bind="gridOptions"></vxe-grid>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { VxeGridProps } from 'vxe-table'
import serviceAxios from '@utils/serviceAxios';

interface RowVO {
  id: number
  name: string
  nickname: string
  role: string
  sex: string
  age: number
  address: string
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
    ajax: {
      query: ({ page }) => {
        // 默认接收 Promise<{ result: [], page: { total: 100 } }>
        return serviceAxios.post('/image/image/pageImages',{"size": page.pageSize,"current": page.currentPage})
      }
    }
  },
  columns: [
    { type: 'seq', width: 70 },
    { field: 'name', title: 'Name' },
    { field: 'fileName', title: 'fileName' },
    { field: 'role', title: 'Role' },
    { field: 'address', title: 'Address', showOverflow: true }
  ]
})
</script>

<script setup lang="ts">
import {ref,PropType} from "vue";
import axios from 'axios'

const item = {
  date: '2016-05-02',
  name: 'Tom',
  address: 'No. 189, Grove St, Los Angeles',
}
interface RowData {
  id: number;
  name: string;
  age: number;
}
const tableData = ref<RowData[]>([])
//const tableData = ref(Array.from({ length: 20 }).fill(item))

const pageOptions = ref({currentPage: 1,
  pageSize: 10,
  total: 0,
  pagerCount: 5,
  layouts: ['PrevPage', 'JumpNumber', 'NextPage', 'FullJump', 'Sizes', 'Total']})


const handleLoad = async (params:any)=> {
  const response = await axios.get('/api/data', {
    params: {
      page: params.page.currentPage,
      pageSize: params.page.pageSize,
      sort: params.sort.field,
      order: params.sort.order,
      filter: params.filter
    }
  });
  tableData.value = response.data.items;
  //this.pageOptions.total = response.data.total;
}
const handlePageChange = ( currentPage:number, pageSize:number ) => {
  pageOptions.value.currentPage = currentPage;
  pageOptions.value.pageSize = pageSize;
  handleLoad({ page: pageOptions.value });
}

</script>

<template>
  <el-scrollbar>
<!--    <el-table :data="tableData">
      <el-table-column prop="date" label="Date" width="140" />
      <el-table-column prop="name" label="Name" width="120" />
      <el-table-column prop="address" label="Address" />
    </el-table>-->
    <vxe-table :data="tableData" @page-change="handlePageChange" :page.sync="pageOptions" :load="handleLoad">
      <vxe-table-column field="name" title="Name"></vxe-table-column>
      <vxe-table-column field="date" title="Date"></vxe-table-column>
      <vxe-table-column field="address" title="Address"></vxe-table-column>
    </vxe-table>
  </el-scrollbar>
</template>

<style scoped>

</style>
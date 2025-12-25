<template>
  <div class="table-wrapper">
    <!-- 查询条件 -->
    <el-form :model="queryParams" label-width="100px" style="margin-bottom: 15px">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="切片名称">
            <el-input v-model="queryParams.slideName" placeholder="请输入切片名称"/>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态">
              <el-option label="全部" :value="null"/>
              <el-option label="可用" :value="1"/>
              <el-option label="不可用" :value="2"/>
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button type="primary" @click="reset">重置</el-button>
          <!-- 可选：上传按钮 -->
          <el-button type="success" @click="openUploadDialog">上传切片</el-button>
          <el-dialog v-model="uploadDialogVisible" title="上传切片" width="50%">
            <MultipartUploader @success="handleUploadSuccess" @close="uploadDialogVisible = false"/>
          </el-dialog>
        </el-col>
      </el-row>
    </el-form>

    <!-- 表格 -->
    <vxe-grid ref="gridRef" v-bind="gridOptions">
      <template #thumbnail="{ row }">
        <el-image
            :src="`/image-service/slide/getThumbnailImage/${row.slideId}`"
            :preview-src-list="[`/image-service/slide/getThumbnailImage/${row.slideId}`]"
            fit="contain" style="width: auto; height: 80px; cursor: pointer"
            @click="previewImage(row)"
        >
        </el-image>
      </template>
      <template #action="{ row }">
        <el-link type="primary" @click="openDetail(row)">查看详情</el-link>&nbsp;&nbsp;
        <el-link type="primary" @click="openMapDialog(row.slideId)">viewer</el-link>&nbsp;&nbsp;
        <el-link type="danger" @click="deleteSlideById(row.slideId)">删除</el-link>
      </template>
    </vxe-grid>
    <map-dialog
        ref="mapDialogRef"
        @ready="handleMapReady"
        @error="handleMapError"
    />
  </div>
</template>

<script lang="ts" setup>
import {ref, reactive} from 'vue'
import MapDialog from '@comps/Map'
import type {VxeGridInstance, VxeGridProps} from 'vxe-table'
import {
  getSlidePage,
  deleteSlide,
  getSlideById
} from '@api/image/image-api'
import {Slide, ImageProcessStatusMap} from '@types/slide'
import {ElMessage} from 'element-plus'
import MultipartUploader from '@comps/MultipartUploader'

const mapDialogRef = ref(null)
const mapDialogVisible = ref(false)

const openMapDialog = (slideId: string) => {
  mapDialogVisible.value = true
  mapDialogRef.value.handleOpen(slideId)
}

const handleMapReady = (map) => {
  console.log('地图已加载:', map)
}

const handleMapError = (err) => {
  console.error('地图加载失败:', err)
}

const uploadDialogVisible = ref(false)

const openUploadDialog = () => {
  uploadDialogVisible.value = true
}

const handleUploadSuccess = () => {
  ElMessage.success('上传成功')
  search()
}


// 查询参数
const queryParams = reactive({
  slideName: '',
  status: null as number | null
})

// 表格引用
const gridRef = ref<VxeGridInstance<Slide>>()

// 搜索方法
const search = () => {
  gridRef.value?.commitProxy('query', {}, queryParams)
}

// 重置方法
const reset = () => {
  queryParams.slideName = ''
  queryParams.status = null
  search()
}

// 查看详情
const openDetail = async (row: Slide) => {
  const res = await getSlideById(row.slideId)
  if (res.code === 200) {
    // 可跳转到详情页或弹出模态框
    console.log('详情数据:', res.data)
  }
}

// 删除切片
const deleteSlideById = async (slideId: number) => {
  try {
    const res = await deleteSlide(slideId)
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

const previewImage = (slideId: string) => {
  // ElMessage.info('点击图片预览')
}

// 表格配置
const gridOptions = reactive<VxeGridProps<Slide>>({
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
        return getSlidePage(Object.assign({}, g.page, form))
      }
    }
  },
  columns: [
    {type: 'seq', width: 60, align: 'center'},
    {
      field: '',
      title: '缩略图',
      width: 100,
      slots: { default: 'thumbnail' }
    },
    {field: 'slideName', title: '切片名称'},
    {field: 'format', title: '格式'},
    {field: 'size', title: '大小(MB)'},
    {
      field: 'status',
      title: '状态',
      formatter: ({cellValue}) => ImageProcessStatusMap[cellValue] || '未知状态'
    },

    {field: 'createTime', title: '上传时间', showOverflow: true},
    {title: '操作', slots: {default: 'action'}, width: 120, align: 'center'}
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

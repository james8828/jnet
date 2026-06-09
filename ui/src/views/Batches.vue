<template>
  <div class="batches-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>批次管理中心</span>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              新建批次
            </el-button>
            <el-button @click="refreshData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索与筛选 -->
      <el-row :gutter="20" class="filter-row">
        <el-col :span="6">
          <el-input v-model="searchText" placeholder="搜索批次编码/名称..." prefix-icon="Search" clearable />
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterProjectId" placeholder="所属项目" clearable style="width: 100%">
            <el-option 
              v-for="project in projects" 
              :key="project.projectId" 
              :label="project.name" 
              :value="project.projectId" 
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="上传状态" clearable style="width: 100%">
            <el-option label="待上传" value="pending" />
            <el-option label="上传中" value="uploading" />
            <el-option label="已完成" value="completed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-col>
      </el-row>

      <!-- 批次表格 -->
      <el-table :data="batches" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="batchCode" label="批次编码" min-width="180">
          <template #default="{ row }">
            <div class="batch-code-cell">
              <el-icon color="#409EFF"><Collection /></el-icon>
              <span>{{ row.batchCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="batchName" label="批次名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="projectName" label="所属项目" width="150" show-overflow-tooltip />
        <el-table-column prop="scannerModel" label="扫描仪型号" width="150" show-overflow-tooltip />
        <el-table-column prop="stainingProtocol" label="染色方案" width="120" show-overflow-tooltip />
        <el-table-column prop="totalImages" label="图像数量" width="120" align="center" />
        <el-table-column prop="uploadStatus" label="上传状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.uploadStatus)" size="small">
              {{ getStatusText(row.uploadStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editBatch(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="showReparseDialog(row)">重新解析</el-button>
            <el-button link type="danger" size="small" @click="deleteBatch(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 新建/编辑批次对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑批次' : '新建批次'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属项目" required>
          <el-select v-model="form.projectId" style="width: 100%" placeholder="请选择项目">
            <el-option 
              v-for="project in projects" 
              :key="project.projectId" 
              :label="project.name" 
              :value="project.projectId" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="批次编码" required>
          <el-input v-model="form.batchCode" placeholder="例如：BATCH_2024_001" />
        </el-form-item>
        <el-form-item label="批次名称">
          <el-input v-model="form.batchName" placeholder="例如：2024年第一批肺癌筛查切片" />
        </el-form-item>
        <el-form-item label="扫描仪型号">
          <el-input v-model="form.scannerModel" placeholder="例如：Aperio AT2" />
        </el-form-item>
        <el-form-item label="染色方案">
          <el-input v-model="form.stainingProtocol" placeholder="例如：HE染色" />
        </el-form-item>
        <el-form-item label="存储路径">
          <el-input v-model="form.storageRootPath" placeholder="例如：/data/batches/2024_001" />
        </el-form-item>
        <el-form-item label="上传状态">
          <el-select v-model="form.uploadStatus" style="width: 100%" placeholder="请选择状态">
            <el-option label="待上传" value="pending" />
            <el-option label="上传中" value="uploading" />
            <el-option label="已完成" value="completed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重新解析对话框 -->
    <el-dialog v-model="reparseDialogVisible" title="重新解析批次" width="600px">
      <div v-if="!isReparsing">
        <el-alert
          title="重新解析将对该批次下所有图像执行元数据提取"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />
        
        <el-form label-width="120px">
          <el-form-item label="批次编码">
            <span>{{ currentBatch?.batchCode }}</span>
          </el-form-item>
          <el-form-item label="批次名称">
            <span>{{ currentBatch?.batchName || '-' }}</span>
          </el-form-item>
          <el-form-item label="图像数量">
            <span>{{ currentBatch?.totalImages || 0 }}</span>
          </el-form-item>
          <el-form-item label="强制解析">
            <el-switch v-model="forceReparse" />
            <span style="margin-left: 10px; color: #909399; font-size: 12px">
              开启后将重新解析已有元数据的图像
            </span>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 解析进度 -->
      <div v-else class="reparse-progress">
        <el-progress 
          :percentage="reparseProgress" 
          :status="reparseStatus"
          :stroke-width="20"
        />
        <div class="reparse-stats" style="margin-top: 20px">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">总数</div>
                <div class="stat-value">{{ reparseResult.totalCount }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item success">
                <div class="stat-label">成功</div>
                <div class="stat-value">{{ reparseResult.successCount }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item warning">
                <div class="stat-label">跳过</div>
                <div class="stat-value">{{ reparseResult.skippedCount }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item error">
                <div class="stat-label">失败</div>
                <div class="stat-value">{{ reparseResult.failedCount }}</div>
              </div>
            </el-col>
          </el-row>
        </div>
        
        <!-- 错误信息 -->
        <div v-if="reparseResult.errorMessages.length > 0" style="margin-top: 20px">
          <el-divider>错误详情</el-divider>
          <el-scrollbar height="200px">
            <div v-for="(msg, index) in reparseResult.errorMessages" :key="index" class="error-message">
              {{ msg }}
            </div>
          </el-scrollbar>
        </div>
      </div>
      
      <template #footer>
        <el-button v-if="!isReparsing" @click="reparseDialogVisible = false">取消</el-button>
        <el-button v-if="!isReparsing" type="primary" @click="confirmReparse">开始解析</el-button>
        <el-button v-else @click="reparseDialogVisible = false" :disabled="reparseProgress < 100">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBatchPage, createBatch, updateBatch, reparseBatch } from '@/api/batches'
import { getProjectPage } from '@/api/projects'
import type { BatchVO, BatchDTO, BatchQueryDTO } from '@/types/batch'
import type { ProjectVO } from '@/types/project'
import { PageData } from '@/utils/request'

const route = useRoute()

const loading = ref(false)
const searchText = ref('')
const filterProjectId = ref<number | undefined>(undefined)
const filterStatus = ref<string | ''>('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentBatchId = ref<number | null>(null)
const projects = ref<ProjectVO[]>([])

// 重新解析相关状态
const reparseDialogVisible = ref(false)
const currentBatch = ref<BatchVO | null>(null)
const forceReparse = ref(false)
const isReparsing = ref(false)
const reparseProgress = ref(0)
const reparseStatus = ref<'success' | 'exception' | undefined>(undefined)
const reparseResult = ref({
  totalCount: 0,
  successCount: 0,
  failedCount: 0,
  skippedCount: 0,
  errorMessages: [] as string[]
})

const form = ref<BatchDTO>({
  projectId: 0,
  batchCode: '',
  batchName: '',
  scannerModel: '',
  stainingProtocol: '',
  storageRootPath: '',
  uploadStatus: 'pending'
})

// 批次列表数据
const batches = ref<BatchVO[]>([])

// 获取状态文本
const getStatusText = (status?: string) => {
  const map: Record<string, string> = { 
    pending: '待上传', 
    uploading: '上传中', 
    completed: '已完成', 
    failed: '失败' 
  }
  return status ? map[status] || status : '-'
}

// 获取状态类型
const getStatusType = (status?: string) => {
  const map: Record<string, any> = { 
    pending: 'info', 
    uploading: 'warning', 
    completed: 'success', 
    failed: 'danger' 
  }
  return status ? map[status] || 'info' : 'info'
}

// 加载项目列表（用于下拉选择）
const loadProjects = async () => {
  try {
    const result = await getProjectPage({
      current: 1,
      size: 100  // 加载所有项目
    })
    console.log('[Batches] 项目列表响应:', result)
    projects.value = result.records || []
    console.log('[Batches] 项目数量:', projects.value.length)
  } catch (error) {
    console.error('加载项目列表失败:', error)
    ElMessage.error('加载项目列表失败')
  }
}

// 加载批次列表
const loadBatches = async () => {
  loading.value = true
  try {
    const query: BatchQueryDTO = {
      current: currentPage.value,
      size: pageSize.value,
      batchCode: searchText.value || undefined,
      batchName: searchText.value || undefined,
      projectId: filterProjectId.value,
      uploadStatus: filterStatus.value || undefined
    }
    
    const result: PageData<BatchVO> = await getBatchPage(query)
    batches.value = result.records
    total.value = result.total
  } catch (error) {
    console.error('加载批次列表失败:', error)
    ElMessage.error('加载批次列表失败')
  } finally {
    loading.value = false
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  currentBatchId.value = null
  form.value = {
    projectId: 0,
    batchCode: '',
    batchName: '',
    scannerModel: '',
    stainingProtocol: '',
    storageRootPath: '',
    uploadStatus: 'pending'
  }
  dialogVisible.value = true
}

// 编辑批次
const editBatch = (row: BatchVO) => {
  isEdit.value = true
  currentBatchId.value = row.batchId
  form.value = {
    projectId: row.projectId,
    batchCode: row.batchCode,
    batchName: row.batchName,
    scannerModel: row.scannerModel,
    stainingProtocol: row.stainingProtocol,
    storageRootPath: row.storageRootPath,
    uploadStatus: row.uploadStatus
  }
  dialogVisible.value = true
}

// 确认保存
const confirmSave = async () => {
  // 表单验证
  if (!form.value.projectId || !form.value.batchCode) {
    ElMessage.warning('请填写所属项目和批次编码')
    return
  }
  
  try {
    if (isEdit.value && currentBatchId.value) {
      await updateBatch(currentBatchId.value, form.value)
      ElMessage.success('批次更新成功')
    } else {
      await createBatch(form.value)
      ElMessage.success('批次创建成功')
    }
    dialogVisible.value = false
    await loadBatches()
  } catch (error) {
    console.error('保存批次失败:', error)
    ElMessage.error(isEdit.value ? '批次更新失败' : '批次创建失败')
  }
}

// 删除批次
const deleteBatch = (row: BatchVO) => {
  ElMessageBox.confirm(
    `确定要删除批次 "${row.batchCode}" 吗？此操作不可恢复。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // TODO: 调用删除API
      ElMessage.success('批次删除成功')
      await loadBatches()
    } catch (error) {
      console.error('删除批次失败:', error)
      ElMessage.error('删除批次失败')
    }
  }).catch(() => {})
}

// 显示重新解析对话框
const showReparseDialog = (row: BatchVO) => {
  currentBatch.value = row
  forceReparse.value = false
  reparseProgress.value = 0
  reparseStatus.value = undefined
  reparseResult.value = {
    totalCount: 0,
    successCount: 0,
    failedCount: 0,
    skippedCount: 0,
    errorMessages: []
  }
  reparseDialogVisible.value = true
}

// 确认重新解析
const confirmReparse = async () => {
  if (!currentBatch.value) return
  
  try {
    await ElMessageBox.confirm(
      `确定要${forceReparse.value ? '强制' : ''}重新解析批次 "${currentBatch.value.batchCode}" 下的所有图像吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return // 用户取消
  }
  
  isReparsing.value = true
  reparseProgress.value = 0
  reparseStatus.value = undefined
  
  try {
    console.log('[confirmReparse] 开始重新解析批次:', currentBatch.value.batchId)
    
    // 调用后端 API
    const result = await reparseBatch(currentBatch.value.batchId, forceReparse.value)
    
    console.log('[confirmReparse] 解析结果:', result)
    
    // 更新结果
    reparseResult.value = result
    
    // 计算进度（模拟）
    reparseProgress.value = 100
    
    if (result.failedCount > 0) {
      reparseStatus.value = 'exception'
      ElMessage.warning(`解析完成，但有 ${result.failedCount} 个图像失败`)
    } else {
      reparseStatus.value = 'success'
      ElMessage.success(`解析完成！成功: ${result.successCount}, 跳过: ${result.skippedCount}`)
    }
  } catch (error) {
    console.error('[confirmReparse] 解析失败:', error)
    reparseStatus.value = 'exception'
    reparseProgress.value = 100
    ElMessage.error('重新解析失败')
  }
}

// 刷新数据
const refreshData = () => {
  loadBatches()
}

// 监听分页变化
const handlePageChange = () => {
  loadBatches()
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadBatches()
}

// 监听筛选条件变化，自动触发查询
watch(
  [searchText, filterProjectId, filterStatus],
  () => {
    // 重置到第一页
    currentPage.value = 1
    loadBatches()
  },
  { deep: false }
)

// 组件挂载时加载数据
onMounted(() => {
  // 检查 URL 参数中是否有 projectId，如果有则自动筛选
  const projectIdParam = route.query.projectId as string
  if (projectIdParam) {
    filterProjectId.value = Number(projectIdParam)
  }
  
  loadProjects()
  loadBatches()
})
</script>

<style scoped lang="scss">
.batches-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
  
  .filter-row {
    margin-bottom: 20px;
  }
  
  .batch-code-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
  }
  
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  
  // 重新解析进度样式
  .reparse-progress {
    .stat-item {
      text-align: center;
      padding: 10px;
      border-radius: 4px;
      background-color: #f5f7fa;
      
      &.success {
        background-color: #f0f9ff;
        color: #67c23a;
      }
      
      &.warning {
        background-color: #fdf6ec;
        color: #e6a23c;
      }
      
      &.error {
        background-color: #fef0f0;
        color: #f56c6c;
      }
      
      .stat-label {
        font-size: 12px;
        color: #909399;
        margin-bottom: 5px;
      }
      
      .stat-value {
        font-size: 24px;
        font-weight: bold;
      }
    }
    
    .error-message {
      padding: 8px 12px;
      margin-bottom: 8px;
      background-color: #fef0f0;
      border-left: 3px solid #f56c6c;
      border-radius: 4px;
      font-size: 12px;
      color: #f56c6c;
    }
  }
}
</style>

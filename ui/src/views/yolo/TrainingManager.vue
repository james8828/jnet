<template>
  <div class="training-manager">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模型训练任务管理</span>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            新建训练任务
          </el-button>
        </div>
      </template>
      
      <!-- 筛选栏 -->
      <el-form inline class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 150px">
            <el-option label="等待中" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型架构">
          <el-select v-model="filterModelArch" placeholder="全部" clearable style="width: 150px">
            <el-option label="YOLOv8n" value="yolov8n" />
            <el-option label="YOLOv8s" value="yolov8s" />
            <el-option label="YOLOv8m" value="yolov8m" />
            <el-option label="YOLOv8l" value="yolov8l" />
            <el-option label="YOLOv8x" value="yolov8x" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTasks">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleResetFilter">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 任务列表 -->
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column prop="taskNo" label="任务编号" width="180" fixed />
        <el-table-column prop="taskName" label="任务名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="modelArchitecture" label="模型架构" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.modelArchitecture }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="200">
          <template #default="{ row }">
            <el-progress 
              :percentage="Math.round(row.progress)" 
              :status="row.status === 'FAILED' ? 'exception' : (row.status === 'SUCCESS' ? 'success' : undefined)"
              :stroke-width="16"
            >
              <template #default="{ percentage }">
                <span class="progress-text">{{ percentage }}%</span>
              </template>
            </el-progress>
          </template>
        </el-table-column>
        <el-table-column label="训练轮数" width="120">
          <template #default="{ row }">
            <div v-if="row.currentEpoch !== undefined">
              {{ row.currentEpoch }} / {{ row.epochs }}
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="最佳指标" width="150">
          <template #default="{ row }">
            <div v-if="row.bestMetrics" class="metrics-info">
              <div>mAP@0.5: {{ row.bestMetrics.map50?.toFixed(3) || '-' }}</div>
              <div>Precision: {{ row.bestMetrics.precision?.toFixed(3) || '-' }}</div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 'RUNNING' || row.status === 'PENDING'" 
              size="small" 
              type="warning"
              @click="handleCancel(row.taskId)"
            >
              取消
            </el-button>
            <el-button 
              v-if="row.status === 'SUCCESS'" 
              size="small" 
              type="primary"
              @click="handleDownloadModel(row.taskId)"
            >
              下载模型
            </el-button>
            <el-button 
              v-if="row.status === 'SUCCESS'" 
              size="small" 
              type="success"
              @click="handleEvaluate(row.taskId)"
            >
              评估
            </el-button>
            <el-button 
              size="small" 
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
            <el-button 
              size="small" 
              type="danger"
              @click="handleDelete(row.taskId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadTasks"
        @size-change="loadTasks"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
    
    <!-- 创建训练任务对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建训练任务"
      width="900px"
      :close-on-click-modal="false"
    >
      <TrainingConfigForm
        ref="trainingFormRef"
        :project-id="actualProjectId"
        @submit="handleCreateTask"
      />
    </el-dialog>
    
    <!-- 任务详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="训练任务详情"
      width="1100px"
    >
      <div v-if="currentTask" class="task-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务编号">
            {{ currentTask.taskNo }}
          </el-descriptions-item>
          <el-descriptions-item label="任务名称">
            {{ currentTask.taskName }}
          </el-descriptions-item>
          <el-descriptions-item label="模型架构">
            <el-tag type="primary">{{ currentTask.modelArchitecture }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask.status)">
              {{ getStatusText(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="进度">
            <el-progress :percentage="Math.round(currentTask.progress)" />
          </el-descriptions-item>
          <el-descriptions-item label="当前轮数">
            {{ currentTask.currentEpoch || '-' }} / {{ currentTask.epochs }}
          </el-descriptions-item>
          <el-descriptions-item label="批次大小">
            {{ currentTask.batchSize }}
          </el-descriptions-item>
          <el-descriptions-item label="图像尺寸">
            {{ currentTask.imageSize }}
          </el-descriptions-item>
          <el-descriptions-item label="学习率">
            {{ currentTask.learningRate }}
          </el-descriptions-item>
          <el-descriptions-item label="预训练权重">
            {{ currentTask.pretrainedWeights || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatTime(currentTask.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="开始时间" :span="2">
            {{ currentTask.startTime ? formatTime(currentTask.startTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="结束时间" :span="2">
            {{ currentTask.endTime ? formatTime(currentTask.endTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="耗时" :span="2">
            {{ currentTask.durationSeconds ? formatDuration(currentTask.durationSeconds) : '-' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <!-- 训练指标 -->
        <div v-if="currentTask.bestMetrics" class="metrics-section">
          <h4>最佳性能指标</h4>
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">mAP@0.5</div>
                <div class="metric-value">{{ currentTask.bestMetrics.map50?.toFixed(3) || '-' }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">mAP@0.5:0.95</div>
                <div class="metric-value">{{ currentTask.bestMetrics.map50_95?.toFixed(3) || '-' }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Precision</div>
                <div class="metric-value">{{ currentTask.bestMetrics.precision?.toFixed(3) || '-' }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Recall</div>
                <div class="metric-value">{{ currentTask.bestMetrics.recall?.toFixed(3) || '-' }}</div>
              </div>
            </el-col>
          </el-row>
        </div>
        
        <!-- 错误信息 -->
        <el-alert
          v-if="currentTask.errorMessage"
          title="错误信息"
          type="error"
          :closable="false"
          show-icon
          style="margin-top: 16px"
        >
          <pre>{{ currentTask.errorMessage }}</pre>
          <pre v-if="currentTask.errorStack" style="margin-top: 8px; font-size: 12px;">{{ currentTask.errorStack }}</pre>
        </el-alert>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import duration from 'dayjs/plugin/duration'
import { 
  listTrainingTasks, 
  createTrainingTask,
  cancelTrainingTask,
  downloadModel,
  deleteTrainingTask,
  evaluateModel,
  type TrainingTask,
  type TrainingTaskCreateRequest
} from '@/api/training-tasks'
import TrainingConfigForm from './components/TrainingConfigForm.vue'

dayjs.extend(duration)

// 从 localStorage 获取当前选中的项目ID
const getCurrentProjectId = (): number => {
  const projectId = localStorage.getItem('currentProjectId')
  if (!projectId) {
    ElMessage.warning('请先选择一个项目')
    return 0
  }
  return Number(projectId)
}

const props = defineProps<{
  projectId?: number
}>()

// 使用传入的 projectId 或从 localStorage 获取
const actualProjectId = computed(() => props.projectId || getCurrentProjectId())

const loading = ref(false)
const tasks = ref<TrainingTask[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')
const filterModelArch = ref('')
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const currentTask = ref<TrainingTask | null>(null)
const trainingFormRef = ref()

// 加载任务列表
const loadTasks = async () => {
  loading.value = true
  try {
    const res = await listTrainingTasks({
      projectId: actualProjectId.value,
      status: filterStatus.value || undefined,
      taskName: undefined,  // 可以添加taskName筛选
      modelArchitecture: filterModelArch.value || undefined,
      current: pageNum.value,
      size: pageSize.value
    })
    tasks.value = res.records
    total.value = res.total
  } catch (error) {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

// 创建任务
const handleCreateTask = async (formData: TrainingTaskCreateRequest) => {
  try {
    await createTrainingTask(formData)
    ElMessage.success('训练任务创建成功')
    showCreateDialog.value = false
    trainingFormRef.value?.resetForm()
    loadTasks()
  } catch (error) {
    ElMessage.error('任务创建失败')
  }
}

// 取消任务
const handleCancel = async (taskId: number) => {
  try {
    await ElMessageBox.confirm('确定要取消该训练任务吗？', '提示', {
      type: 'warning'
    })
    await cancelTrainingTask(taskId)
    ElMessage.success('任务已取消')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// 下载模型
const handleDownloadModel = async (taskId: number) => {
  try {
    const blob = await downloadModel(taskId)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `model-${taskId}.pt`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('开始下载模型')
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

// 评估模型
const handleEvaluate = async (taskId: number) => {
  try {
    await ElMessageBox.confirm('确定要评估该模型吗？这可能需要一些时间。', '提示', {
      type: 'info'
    })
    await evaluateModel(taskId)
    ElMessage.success('评估任务已提交')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('评估失败')
    }
  }
}

// 查看详情
const handleViewDetail = (task: TrainingTask) => {
  currentTask.value = task
  showDetailDialog.value = true
}

// 删除任务
const handleDelete = async (taskId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？此操作不可恢复！', '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await deleteTrainingTask(taskId)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 重置筛选
const handleResetFilter = () => {
  filterStatus.value = ''
  filterModelArch.value = ''
  pageNum.value = 1
  loadTasks()
}

// 获取状态类型
const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    PENDING: 'info',
    RUNNING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '执行中',
    SUCCESS: '成功',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 格式化时长
const formatDuration = (seconds: number) => {
  const dur = dayjs.duration(seconds, 'seconds')
  const hours = Math.floor(dur.asHours())
  const minutes = dur.minutes()
  const secs = dur.seconds()
  
  if (hours > 0) {
    return `${hours}小时${minutes}分${secs}秒`
  } else if (minutes > 0) {
    return `${minutes}分${secs}秒`
  } else {
    return `${secs}秒`
  }
}

onMounted(() => {
  loadTasks()
})
</script>

<style scoped lang="scss">
.training-manager {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    font-size: 16px;
  }
  
  .filter-form {
    margin-bottom: 16px;
  }
  
  .progress-text {
    font-size: 12px;
    color: #606266;
  }
  
  .metrics-info {
    font-size: 12px;
    color: #606266;
    line-height: 1.6;
  }
  
  .task-detail {
    .metrics-section {
      margin-top: 20px;
      
      h4 {
        margin-bottom: 16px;
        font-size: 14px;
        font-weight: 600;
      }
      
      .metric-card {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        padding: 20px;
        border-radius: 8px;
        color: white;
        text-align: center;
        
        .metric-label {
          font-size: 14px;
          opacity: 0.9;
          margin-bottom: 8px;
        }
        
        .metric-value {
          font-size: 24px;
          font-weight: 600;
        }
      }
    }
    
    pre {
      margin: 0;
      white-space: pre-wrap;
      word-break: break-all;
      font-family: 'Consolas', 'Monaco', monospace;
    }
  }
}
</style>

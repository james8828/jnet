<template>
  <div class="dataset-builder">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据集构建任务管理</span>
          <el-button type="primary" @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            新建任务
          </el-button>
        </div>
      </template>
      
      <!-- 筛选栏 -->
      <el-form inline class="filter-form">
        <el-form-item label="项目">
          <el-select 
            v-model="filterProjectId" 
            placeholder="全部项目" 
            clearable 
            style="width: 200px"
            @change="handleProjectChange"
          >
            <el-option
              v-for="project in projects"
              :key="project.projectId"
              :label="project.name"
              :value="project.projectId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称">
          <el-input 
            v-model="filterTaskName" 
            placeholder="请输入任务名称" 
            clearable 
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 150px">
            <el-option label="等待中" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前步骤">
          <el-input 
            v-model="filterCurrentStep" 
            placeholder="请输入步骤关键词" 
            clearable 
            style="width: 200px"
          />
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
        <el-table-column prop="currentStep" label="当前步骤" width="180" show-overflow-tooltip />
        <el-table-column label="数据统计" width="180">
          <template #default="{ row }">
            <div v-if="row.totalImages !== undefined" class="stats-info">
              <div>图像: {{ row.totalImages }}</div>
              <div>标注: {{ row.totalAnnotations || 0 }}</div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="数据集划分" width="180">
          <template #default="{ row }">
            <div v-if="row.trainCount !== undefined" class="split-info">
              <el-tag size="small" type="success">训:{{ row.trainCount }}</el-tag>
              <el-tag size="small" type="warning">验:{{ row.valCount }}</el-tag>
              <el-tag size="small" type="info" v-if="row.testCount > 0">测:{{ row.testCount }}</el-tag>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
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
              @click="handleDownload(row.taskId)"
            >
              下载
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
    
    <!-- 创建任务对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建数据集构建任务"
      width="900px"
      :close-on-click-modal="false"
    >
      <DatasetConfigForm
        ref="configFormRef"
        :show-batch-select="true"
        @submit="handleCreateTask"
      />
    </el-dialog>
    
    <!-- 任务详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="任务详情"
      width="1000px"
    >
      <div v-if="currentTask" class="task-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务编号">
            {{ currentTask.taskNo }}
          </el-descriptions-item>
          <el-descriptions-item label="任务名称">
            {{ currentTask.taskName }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask.status)">
              {{ getStatusText(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="进度">
            <el-progress :percentage="Math.round(currentTask.progress)" />
          </el-descriptions-item>
          <el-descriptions-item label="当前步骤" :span="2">
            {{ currentTask.currentStep || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="总图像数">
            {{ currentTask.totalImages || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="总标注数">
            {{ currentTask.totalAnnotations || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="训练集数量">
            {{ currentTask.trainCount || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="验证集数量">
            {{ currentTask.valCount || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="测试集数量">
            {{ currentTask.testCount || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="数据集大小">
            {{ formatFileSize(currentTask.datasetSize) }}
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
        
        <!-- 类别分布 -->
        <div v-if="currentTask.classDistribution" class="class-distribution">
          <h4>类别分布</h4>
          <el-table :data="getClassDistributionData()" size="small" border>
            <el-table-column prop="className" label="类别名称" />
            <el-table-column prop="count" label="数量" width="120" />
            <el-table-column label="占比" width="120">
              <template #default="{ row }">
                {{ ((row.count / (currentTask.totalAnnotations || 1)) * 100).toFixed(1) }}%
              </template>
            </el-table-column>
          </el-table>
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import duration from 'dayjs/plugin/duration'
import { 
  listDatasetTasks, 
  createDatasetTask,
  cancelDatasetTask,
  downloadDataset,
  deleteDatasetTask,
  type DatasetTask,
  type DatasetTaskCreateRequest
} from '@/api/dataset-tasks'
import { getBatchesByProject } from '@/api/batches'
import { getAllProjects } from '@/api/projects'
import DatasetConfigForm from '@/components/DatasetConfigForm.vue'
import { useTaskProgress } from '@/composables/useTaskProgress'

dayjs.extend(duration)

const route = useRoute()

const loading = ref(false)
const tasks = ref<DatasetTask[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterProjectId = ref<number | undefined>(undefined)
const filterTaskName = ref('')
const filterStatus = ref('')
const filterCurrentStep = ref('')
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const currentTask = ref<DatasetTask | null>(null)
const configFormRef = ref()
const projects = ref<any[]>([])

// 监听运行中的任务进度
const runningTaskIds = ref<number[]>([])
const progressWatchers = new Map<number, ReturnType<typeof useTaskProgress>>()

// 加载项目列表
const loadProjects = async () => {
  try {
    console.log('[DatasetBuilder] ========== 开始加载项目列表 ==========')
    console.log('[DatasetBuilder] 调用 getAllProjects API...')
    
    const res = await getAllProjects()
    
    console.log('[DatasetBuilder] API 响应完整数据:', JSON.stringify(res, null, 2))
    console.log('[DatasetBuilder] res 类型:', typeof res)
    console.log('[DatasetBuilder] res 是否为数组:', Array.isArray(res))
    console.log('[DatasetBuilder] 项目数量:', res?.length || 0)
    
    // getAllProjects 直接返回 ProjectVO[] 数组
    projects.value = Array.isArray(res) ? res : []
    
    console.log('[DatasetBuilder] 最终项目列表:', projects.value)
    console.log('[DatasetBuilder] ========== 项目列表加载完成 ==========')
    
    // 如果没有选择项目，默认选中第一个
    if (!filterProjectId.value && projects.value.length > 0) {
      filterProjectId.value = projects.value[0].projectId
      console.log('[DatasetBuilder] 自动选中第一个项目:', filterProjectId.value)
    }
  } catch (error) {
    console.error('[DatasetBuilder] ❌ 加载项目列表失败:', error)
    console.error('[DatasetBuilder] 错误详情:', error instanceof Error ? error.message : error)
    ElMessage.error('加载项目列表失败，请查看控制台')
  }
}

// 项目变化时重置批次和标签
const handleProjectChange = () => {
  // 清空其他筛选条件
  filterTaskName.value = ''
  filterStatus.value = ''
  filterCurrentStep.value = ''
  pageNum.value = 1
  loadTasks()
}

// 加载任务列表
const loadTasks = async () => {
  loading.value = true
  try {
    const res = await listDatasetTasks({
      projectId: filterProjectId.value || undefined,
      taskName: filterTaskName.value || undefined,
      status: filterStatus.value || undefined,
      currentStep: filterCurrentStep.value || undefined,
      current: pageNum.value,
      size: pageSize.value
    })
    tasks.value = res.records  // 修改：从 list 改为 records
    total.value = res.total
    
    // 更新进度监听
    updateProgressWatchers()
  } catch (error) {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

// 更新进度监听器
const updateProgressWatchers = () => {
  // 找出所有运行中的任务
  const newRunningIds = tasks.value
    .filter(t => t.status === 'RUNNING' || t.status === 'PENDING')
    .map(t => t.taskId)
  
  // 移除不再运行的任务监听
  progressWatchers.forEach((watcher, taskId) => {
    if (!newRunningIds.includes(taskId)) {
      watcher.close()
      progressWatchers.delete(taskId)
    }
  })
  
  // 添加新的任务监听
  newRunningIds.forEach(taskId => {
    if (!progressWatchers.has(taskId)) {
      const watcher = useTaskProgress(taskId)
      progressWatchers.set(taskId, watcher)
      
      // 监听进度变化，定期刷新列表
      setInterval(() => {
        if (watcher.status.value === 'SUCCESS' || 
            watcher.status.value === 'FAILED' || 
            watcher.status.value === 'CANCELLED') {
          loadTasks()
        }
      }, 3000)
    }
  })
}

// 创建任务
const handleCreateTask = async (formData: DatasetTaskCreateRequest) => {
  try {
    await createDatasetTask(formData)
    ElMessage.success('任务创建成功')
    showCreateDialog.value = false
    configFormRef.value?.resetForm()
    loadTasks()
  } catch (error) {
    ElMessage.error('任务创建失败')
  }
}

// 取消任务
const handleCancel = async (taskId: number) => {
  try {
    await ElMessageBox.confirm('确定要取消该任务吗？', '提示', {
      type: 'warning'
    })
    await cancelDatasetTask(taskId)
    ElMessage.success('任务已取消')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// 下载数据集
const handleDownload = async (taskId: number) => {
  try {
    const blob = await downloadDataset(taskId)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `dataset-${taskId}.zip`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('开始下载')
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

// 查看详情
const handleViewDetail = (task: DatasetTask) => {
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
    await deleteDatasetTask(taskId)
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
  filterProjectId.value = projects.value.length > 0 ? projects.value[0].projectId : undefined
  filterTaskName.value = ''
  filterStatus.value = ''
  filterCurrentStep.value = ''
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

// 格式化文件大小
const formatFileSize = (bytes?: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
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

// 获取类别分布数据
const getClassDistributionData = () => {
  if (!currentTask.value?.classDistribution) return []
  
  return Object.entries(currentTask.value.classDistribution).map(([className, count]) => ({
    className,
    count
  })).sort((a, b) => b.count - a.count)
}

onMounted(() => {
  loadProjects()
  loadTasks()
})
</script>

<style scoped lang="scss">
.dataset-builder {
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
  
  .stats-info {
    font-size: 12px;
    color: #606266;
    line-height: 1.6;
  }
  
  .split-info {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
  }
  
  .task-detail {
    .class-distribution {
      margin-top: 20px;
      
      h4 {
        margin-bottom: 12px;
        font-size: 14px;
        font-weight: 600;
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

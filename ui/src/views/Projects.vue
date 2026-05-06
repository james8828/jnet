<template>
  <div class="projects-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>项目管理中心</span>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              新建项目
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
          <el-input v-model="searchText" placeholder="搜索项目名称/负责人..." prefix-icon="Search" clearable />
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="项目状态" clearable style="width: 100%">
            <el-option label="进行中" value="active" />
            <el-option label="已完成" value="completed" />
            <el-option label="已归档" value="archived" />
          </el-select>
        </el-col>
      </el-row>

      <!-- 项目表格 -->
      <el-table :data="projects" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="项目名称" min-width="200">
          <template #default="{ row }">
            <div class="project-name-cell">
              <el-icon color="#409EFF"><FolderOpened /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="项目编码" width="150" />
        <el-table-column prop="privacyLevel" label="隐私级别" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getPrivacyLevelType(row.privacyLevel)" size="small">
              {{ getPrivacyLevelText(row.privacyLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalImages" label="图像总量" width="120" align="center" />
        <el-table-column prop="annotatedCount" label="标注进度" width="200">
          <template #default="{ row }">
            <div class="progress-wrapper">
              <el-progress 
                :percentage="Math.round((row.annotatedCount / row.totalImages) * 100)" 
                :stroke-width="8"
              />
              <span class="progress-text">{{ row.annotatedCount }} / {{ row.totalImages }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewProject(row)">查看数据</el-button>
            <el-button link type="primary" size="small" @click="editProject(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteProject(row)">删除</el-button>
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

    <!-- 新建/编辑项目对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑项目' : '新建项目'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="例如：2024肺癌筛查专项" />
        </el-form-item>
        <el-form-item label="项目编码" required>
          <el-input v-model="form.code" placeholder="例如：PROJECT_2024_001" />
        </el-form-item>
        <el-form-item label="隐私级别">
          <el-select v-model="form.privacyLevel" style="width: 100%" placeholder="请选择隐私级别">
            <el-option label="公开" :value="1" />
            <el-option label="脱敏" :value="2" />
            <el-option label="绝密" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="伦理批件号">
          <el-input v-model="form.ethicsCode" placeholder="例如：ETHICS-2024-001" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="简要描述项目目标与范围" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProjectPage, createProject, updateProject, archiveProject, getProjectStats } from '@/api/projects'
import type { ProjectVO, ProjectDTO, ProjectQueryDTO } from '@/types/project'
import { PageData } from '@/utils/request'

const loading = ref(false)
const searchText = ref('')
const filterStatus = ref<string | ''>('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentProjectId = ref<number | null>(null)

const form = ref<ProjectDTO>({
  name: '',
  code: '',
  managerId: undefined,
  ethicsCode: '',
  privacyLevel: undefined,
  description: '',
  targetClasses: undefined,
  status: 'active'  // 默认为 active（小写）
})

// 项目列表数据
const projects = ref<ProjectVO[]>([])

// 获取状态文本
const getStatusText = (status?: string) => {
  const map: Record<string, string> = { 
    active: '进行中', 
    archived: '已归档', 
    deleted: '已删除' 
  }
  return status ? map[status] || status : '-'
}

// 获取状态类型
const getStatusType = (status?: string) => {
  const map: Record<string, any> = { 
    active: 'success', 
    archived: 'info', 
    deleted: 'danger' 
  }
  return status ? map[status] || 'info' : 'info'
}

// 获取隐私级别文本
const getPrivacyLevelText = (level?: number) => {
  const map: Record<number, string> = { 
    1: '公开', 
    2: '脱敏', 
    3: '绝密' 
  }
  return level ? map[level] || '-' : '-'
}

// 获取隐私级别标签类型
const getPrivacyLevelType = (level?: number) => {
  const map: Record<number, any> = { 
    1: 'success',   // 公开 - 绿色
    2: 'warning',   // 脱敏 - 橙色
    3: 'danger'     // 绝密 - 红色
  }
  return level ? map[level] || 'info' : 'info'
}

// 加载项目列表
const loadProjects = async () => {
  loading.value = true
  try {
    const query: ProjectQueryDTO = {
      current: currentPage.value,
      size: pageSize.value,
      name: searchText.value || undefined,
      status: filterStatus.value || undefined
    }
    
    const result: PageData<ProjectVO> = await getProjectPage(query)
    projects.value = result.records
    total.value = result.total
  } catch (error) {
    console.error('加载项目列表失败:', error)
    ElMessage.error('加载项目列表失败')
  } finally {
    loading.value = false
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  currentProjectId.value = null
  form.value = {
    name: '',
    code: '',
    managerId: undefined,
    ethicsCode: '',
    privacyLevel: undefined,
    description: '',
    targetClasses: undefined,
    status: 'active'  // 默认为 active（小写）
  }
  dialogVisible.value = true
}

// 编辑项目
const editProject = (row: ProjectVO) => {
  isEdit.value = true
  currentProjectId.value = row.projectId
  form.value = {
    name: row.name,
    code: row.code,
    managerId: row.managerId,
    ethicsCode: row.ethicsCode,
    privacyLevel: row.privacyLevel,
    description: row.description,
    targetClasses: row.targetClasses,
    status: row.status
  }
  dialogVisible.value = true
}

// 确认保存
const confirmSave = async () => {
  // 表单验证
  if (!form.value.name || !form.value.code) {
    ElMessage.warning('请填写项目名称和编码')
    return
  }
  
  try {
    if (isEdit.value && currentProjectId.value) {
      await updateProject(currentProjectId.value, form.value)
      ElMessage.success('项目更新成功')
    } else {
      await createProject(form.value)
      ElMessage.success('项目创建成功')
    }
    dialogVisible.value = false
    await loadProjects()
  } catch (error) {
    console.error('保存项目失败:', error)
    ElMessage.error(isEdit.value ? '项目更新失败' : '项目创建失败')
  }
}

// 查看项目
const viewProject = (row: ProjectVO) => {
  ElMessage.info(`正在进入项目: ${row.name}`)
  // 实际业务中这里应跳转到 Dataset 页面并携带 projectId 参数
  // router.push({ path: '/dataset', query: { projectId: row.projectId } })
}

// 删除项目（归档）
const deleteProject = (row: ProjectVO) => {
  ElMessageBox.confirm(
    `确定要归档项目 "${row.name}" 吗？此操作将软删除项目。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await archiveProject(row.projectId)
      ElMessage.success('项目已归档')
      await loadProjects()
    } catch (error) {
      console.error('归档项目失败:', error)
      ElMessage.error('归档项目失败')
    }
  }).catch(() => {})
}

// 刷新数据
const refreshData = () => {
  loadProjects()
}

// 监听分页变化
const handlePageChange = () => {
  loadProjects()
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadProjects()
}

// 组件挂载时加载数据
onMounted(() => {
  loadProjects()
})
</script>

<style scoped lang="scss">
.projects-container {
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
  
  .project-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
  }
  
  .progress-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .progress-text {
      font-size: 12px;
      color: #909399;
      white-space: nowrap;
    }
  }
  
  .score-high {
    color: #67C23A;
    font-weight: 600;
  }
  
  .text-muted {
    color: #C0C4CC;
  }
  
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>

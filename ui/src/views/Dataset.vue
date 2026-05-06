<template>
  <div class="dataset-pool-container">
    <el-row :gutter="20" class="main-layout">
      <!-- 左侧项目管理列表 -->
      <el-col :span="5">
        <el-card class="nav-card">
          <template #header>
            <div class="nav-header">
              <span>项目与批次管理</span>
              <div class="header-actions">
                <el-button type="primary" link size="small" @click="showCreateProjectDialog">新建项目</el-button>
                <el-button type="success" link size="small" @click="showCreateBatchDialog" :disabled="!currentProject">新建批次</el-button>
              </div>
            </div>
          </template>
          
          <el-input
            v-model="projectSearch"
            placeholder="搜索项目名称..."
            prefix-icon="Search"
            clearable
            size="small"
            style="margin-bottom: 12px"
          />

          <el-table
            :data="filteredProjects"
            highlight-current-row
            @current-change="handleProjectChange"
            height="calc(100vh - 280px)"
            size="small"
          >
            <el-table-column prop="name" label="项目名称" min-width="120">
              <template #default="{ row }">
                <div class="project-name-cell">
                  <el-icon color="#409EFF"><FolderOpened /></el-icon>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <el-divider />
          
          <!-- 批次筛选 -->
          <div v-if="batches.length > 0" class="batch-filter">
            <div class="filter-title">
              批次筛选
              <el-button type="primary" link size="small" @click="showCreateBatchDialog" style="float: right">+ 新建</el-button>
            </div>
            <el-select v-model="currentBatch" placeholder="选择批次" clearable style="width: 100%" @change="handleBatchChange(currentBatch?.batchId)">
              <el-option
                v-for="batch in batches"
                :key="batch.batchId"
                :label="`${batch.batchCode} - ${batch.batchName || '未命名'}`"
                :value="batch"
              />
            </el-select>
          </div>
          
          <div class="tag-filter">
            <div class="filter-title">全局标签筛选</div>
            <el-tag
              v-for="tag in tags"
              :key="tag.tagId"
              :type="selectedTags.includes(tag.tagId) ? 'primary' : 'info'"
              class="filter-tag"
              @click="toggleTag(tag.tagId)"
            >
              {{ tag.name }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧内容区 -->
      <el-col :span="19">
        <el-card class="content-card">
          <template #header>
            <div class="content-header">
              <div class="view-toggles">
                <el-radio-group v-model="currentView" size="small">
                  <el-radio-button label="table">
                    <el-icon><List /></el-icon> 表格
                  </el-radio-button>
                  <el-radio-button label="grid">
                    <el-icon><Grid /></el-icon> 网格
                  </el-radio-button>
                </el-radio-group>
              </div>
              
              <div class="header-tools">
                <el-input
                  v-model="searchText"
                  placeholder="搜索文件名/病理号..."
                  prefix-icon="Search"
                  clearable
                  style="width: 240px; margin-right: 12px"
                />
                <el-button type="primary" @click="showImportDialog">
                  <el-icon><Upload /></el-icon> 导入
                </el-button>
                <el-dropdown split-button type="success" @command="handleBatchCommand">
                  批量操作
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="export">导出选中</el-dropdown-item>
                      <el-dropdown-item command="label">分配标注</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除选中</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </template>

          <!-- 高级筛选栏 -->
          <div class="advanced-filter">
            <el-select v-model="filterCategory" placeholder="组织类型" clearable style="width: 120px">
              <el-option label="肺部" value="lung" />
              <el-option label="肝脏" value="liver" />
              <el-option label="乳腺" value="breast" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="标注状态" clearable style="width: 120px">
              <el-option label="未标注" value="unannotated" />
              <el-option label="标注中" value="annotating" />
              <el-option label="已完成" value="completed" />
            </el-select>
            <el-select v-model="filterFormat" placeholder="文件格式" clearable style="width: 120px">
              <el-option label="SVS" value="svs" />
              <el-option label="JPG" value="jpg" />
            </el-select>
          </div>

          <!-- 视图展示区 -->
          <div class="view-area">
            <!-- 表格视图 -->
            <el-table
              v-if="currentView === 'table'"
              :data="filteredImages"
              stripe
              v-loading="loadingImages"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column label="缩略图" width="100" align="center">
                <template #default="{ row }">
                  <div class="thumbnail-cell" @click="viewImage(row)" style="cursor: pointer;">
                    <img 
                      :src="getThumbnailUrl(row.imageId, 80)" 
                      :alt="row.originalFilename" 
                      class="table-thumbnail"
                      @error="handleImageError"
                    />
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="originalFilename" label="图像名称" min-width="200">
                <template #default="{ row }">
                  <span>{{ row.originalFilename }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="pathologyId" label="病理报告号" width="140" />
              <el-table-column prop="patientId" label="患者ID" width="120" />
              <el-table-column prop="format" label="格式" width="80" />
              <el-table-column prop="lifecycleStatus" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.lifecycleStatus)" size="small">{{ getStatusText(row.lifecycleStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="fileSize" label="大小" width="100">
                <template #default="{ row }">
                  {{ formatFileSize(row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column prop="annotationProgress" label="标注进度" width="150">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="row.annotationProgress || 0" 
                    :stroke-width="6"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="上传时间" width="180" />
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewImage(row)">预览</el-button>
                  <el-button link type="primary" size="small" @click="annotateImage(row)">标注</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 网格视图 -->
            <div v-else class="grid-view" v-loading="loadingImages">
              <div
                v-for="img in filteredImages"
                :key="img.imageId"
                class="grid-item"
                :class="{ selected: selectedRows.includes(img) }"
                @click="toggleSelect(img)"
              >
                <div class="grid-thumb" @click.stop="viewImage(img)">
                  <img 
                    :src="getThumbnailUrl(img.imageId, 200)" 
                    :alt="img.originalFilename" 
                    class="thumb-img" 
                    @error="handleImageError"
                    @load="console.log('[Thumbnail] 加载成功:', img.imageId, img.originalFilename)"
                  />
                  <div class="grid-overlay">
                    <el-tag size="small" :type="getStatusType(img.lifecycleStatus)">{{ getStatusText(img.lifecycleStatus) }}</el-tag>
                  </div>
                </div>
                <div class="grid-info">
                  <div class="grid-name" :title="img.originalFilename">{{ img.originalFilename }}</div>
                  <div class="grid-meta">{{ img.format }} | {{ formatFileSize(img.fileSize) }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, sizes, prev, pager, next"
            class="pagination-bar"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建批次对话框 -->
    <el-dialog v-model="batchDialogVisible" title="新建批次" width="600px">
      <el-form :model="batchForm" label-width="120px">
        <el-form-item label="所属项目">
          <el-input :value="currentProject?.name" disabled />
        </el-form-item>
        <el-form-item label="批次编号" required>
          <el-input v-model="batchForm.batchCode" placeholder="例如：BATCH_2024_001" />
        </el-form-item>
        <el-form-item label="批次名称">
          <el-input v-model="batchForm.batchName" placeholder="例如：2024年第一季度采集" />
        </el-form-item>
        <el-form-item label="扫描仪型号">
          <el-input v-model="batchForm.scannerModel" placeholder="例如：Hamamatsu S360" />
        </el-form-item>
        <el-form-item label="染色协议">
          <el-input v-model="batchForm.stainingProtocol" placeholder="例如：H&E, IHC" />
        </el-form-item>
        <el-form-item label="存储路径">
          <el-input v-model="batchForm.storageRootPath" placeholder="例如：/data/pathology/batch001" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateBatch">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传图像对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传图像" width="800px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-width="120px">
        <el-form-item label="所属项目">
          <el-input :value="currentProject?.name" disabled />
        </el-form-item>
        <el-form-item label="所属批次" required>
          <el-select v-model="uploadForm.batchId" placeholder="请选择批次" style="width: 100%" :disabled="batches.length === 0">
            <el-option
              v-for="batch in batches"
              :key="batch.batchId"
              :label="`${batch.batchCode} - ${batch.batchName || '未命名'}`"
              :value="batch.batchId"
            />
          </el-select>
          <div v-if="batches.length === 0" style="color: #f56c6c; font-size: 12px; margin-top: 4px">
            请先创建批次
          </div>
        </el-form-item>
        <el-form-item label="病理号">
          <el-input v-model="uploadForm.pathologyId" placeholder="例如：P2024-001" />
        </el-form-item>
        <el-form-item label="患者ID">
          <el-input v-model="uploadForm.patientId" placeholder="例如：PATIENT_001" />
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="10"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            accept=".svs,.ndpi,.jpg,.jpeg,.png,.tiff,.tif"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 SVS、NDPI、JPG、PNG、TIFF 格式，单个文件不超过 10GB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      
      <!-- 上传进度 -->
      <div v-if="uploadingFiles.length > 0" class="upload-progress">
        <div v-for="file in uploadingFiles" :key="file.name" class="progress-item">
          <div class="progress-info">
            <span class="filename">{{ file.name }}</span>
            <span class="percentage">{{ file.percentage }}%</span>
          </div>
          <el-progress :percentage="file.percentage" :status="file.status" />
        </div>
      </div>
      
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="startUpload" :loading="isUploading" :disabled="!canUpload">
          {{ isUploading ? '上传中...' : '开始上传' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getProjectPage } from '@/api/projects'
import { getBatchesByProject, createBatch } from '@/api/batches'
import { searchImages, getThumbnailUrl } from '@/api/images'
import { getAllTags } from '@/api/tags'
import type { ProjectVO } from '@/types/project'
import type { BatchVO, BatchDTO } from '@/types/batch'
import type { Image, ImageQueryDTO } from '@/types/image'
import type { Tag } from '@/types/tag'
import { PageData } from '@/utils/request'
import ChunkUploader from '@/utils/chunk-uploader'
import type { UploadUserFile } from 'element-plus'

const currentView = ref('grid')  // 默认使用网格视图（显示缩略图）
const searchText = ref('')
const filterCategory = ref('')
const filterStatus = ref<string | ''>('')
const filterFormat = ref<string | ''>('')
const selectedTags = ref<number[]>([])
const selectedRows = ref<Image[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const projectSearch = ref('')
const currentProject = ref<ProjectVO | null>(null)
const currentBatch = ref<BatchVO | null>(null)

const router = useRouter()

// 批次对话框
const batchDialogVisible = ref(false)
const batchForm = ref<BatchDTO>({
  projectId: 0,
  batchCode: '',
  batchName: '',
  scannerModel: '',
  stainingProtocol: '',
  storageRootPath: ''
})

// 上传对话框
const uploadDialogVisible = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
const uploadingFiles = ref<Array<{ name: string; percentage: number; status?: 'success' | 'exception' }>>([])
const isUploading = ref(false)
const uploadForm = ref({
  batchId: undefined as number | undefined,
  pathologyId: '',
  patientId: ''
})

// 是否可以上传
const canUpload = computed(() => {
  return uploadForm.value.batchId && fileList.value.length > 0 && !isUploading.value
})

// 项目列表
const projects = ref<ProjectVO[]>([])
const batches = ref<BatchVO[]>([])

// 图像列表
const images = ref<Image[]>([])

// 标签列表
const tags = ref<Tag[]>([])

// 加载状态
const loadingProjects = ref(false)
const loadingImages = ref(false)

// 过滤后的项目列表
const filteredProjects = computed(() => {
  return projects.value.filter(p => 
    p.name.toLowerCase().includes(projectSearch.value.toLowerCase())
  )
})

// 过滤后的图像列表（前端二次过滤）
const filteredImages = computed(() => {
  return images.value.filter(img => {
    const matchSearch = !searchText.value || 
      img.originalFilename.toLowerCase().includes(searchText.value.toLowerCase()) ||
      (img.pathologyId && img.pathologyId.includes(searchText.value))
    return matchSearch
  })
})

// 加载项目列表
const loadProjects = async () => {
  loadingProjects.value = true
  try {
    const result = await getProjectPage({
      current: 1,
      size: 100,
      status: 'active'  // 小写
    })
    projects.value = result.records
  } catch (error) {
    console.error('加载项目列表失败:', error)
    ElMessage.error('加载项目列表失败')
  } finally {
    loadingProjects.value = false
  }
}

// 加载批次列表
const loadBatches = async (projectId: number) => {
  try {
    batches.value = await getBatchesByProject(projectId)
  } catch (error) {
    console.error('加载批次列表失败:', error)
  }
}

// 加载图像列表
const loadImages = async () => {
  if (!currentProject.value) {
    images.value = []
    console.log('[loadImages] 未选择项目，清空图像列表')
    return
  }
  
  loadingImages.value = true
  try {
    const query: ImageQueryDTO = {
      current: currentPage.value,
      size: pageSize.value,
      projectId: currentProject.value.projectId,
      batchId: currentBatch.value?.batchId,
      keyword: searchText.value || undefined,
      lifecycleStatus: filterStatus.value || undefined,
      format: filterFormat.value || undefined
    }
    
    console.log('[loadImages] 查询参数:', query)
    const result: PageData<Image> = await searchImages(query)
    images.value = result.records
    total.value = result.total
    console.log('[loadImages] 加载成功, 总数:', total.value, ', 当前页数量:', images.value.length)
    
    // 输出第一条图像的详细信息
    if (images.value.length > 0) {
      const firstImage = images.value[0]
      console.log('[loadImages] 示例图像数据:', {
        imageId: firstImage.imageId,
        filename: firstImage.filename,
        originalFilename: firstImage.originalFilename,
        format: firstImage.format,
        fileSize: firstImage.fileSize,
        thumbnailUrl: firstImage.thumbnailUrl  // 修复：使用正确的字段名
      })
    }
  } catch (error) {
    console.error('加载图像列表失败:', error)
    ElMessage.error('加载图像列表失败')
  } finally {
    loadingImages.value = false
  }
}

// 加载标签列表
const loadTags = async () => {
  try {
    tags.value = await getAllTags()
  } catch (error) {
    console.error('加载标签列表失败:', error)
  }
}

// 项目切换
const handleProjectChange = (val: ProjectVO | null) => {
  currentProject.value = val
  currentBatch.value = null
  currentPage.value = 1
  
  if (val) {
    ElMessage.info(`已切换至项目: ${val.name}`)
    loadBatches(val.projectId)
    loadImages()
  }
}

// 批次切换
const handleBatchChange = (batchId: number) => {
  currentBatch.value = batches.value.find(b => b.batchId === batchId) || null
  currentPage.value = 1
  loadImages()
}

// 显示创建项目对话框
const showCreateProjectDialog = () => {
  ElMessage.info('请前往“项目管理”页面创建新项目')
}

// 显示创建批次对话框
const showCreateBatchDialog = () => {
  if (!currentProject.value) {
    ElMessage.warning('请先选择一个项目')
    return
  }
  
  batchForm.value = {
    projectId: currentProject.value.projectId,
    batchCode: '',
    batchName: '',
    scannerModel: '',
    stainingProtocol: '',
    storageRootPath: ''
  }
  batchDialogVisible.value = true
}

// 确认创建批次
const confirmCreateBatch = async () => {
  // 表单验证
  if (!batchForm.value.batchCode) {
    ElMessage.warning('请填写批次编号')
    return
  }
  
  try {
    await createBatch(batchForm.value)
    ElMessage.success('批次创建成功')
    batchDialogVisible.value = false
    
    // 重新加载批次列表
    if (currentProject.value) {
      await loadBatches(currentProject.value.projectId)
    }
  } catch (error) {
    console.error('创建批次失败:', error)
    ElMessage.error('创建批次失败')
  }
}

// 切换标签筛选
const toggleTag = (tagId: number) => {
  const index = selectedTags.value.indexOf(tagId)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tagId)
  }
  // TODO: 根据标签过滤图像
}

// 表格选择变化
const handleSelectionChange = (val: Image[]) => {
  selectedRows.value = val
}

// 网格视图选择
const toggleSelect = (img: Image) => {
  const index = selectedRows.value.indexOf(img)
  if (index > -1) {
    selectedRows.value.splice(index, 1)
  } else {
    selectedRows.value.push(img)
  }
}

// 获取图标颜色
const getIconColor = (format?: ImageFormat) => {
  return format === 'SVS' ? '#409EFF' : '#67C23A'
}

// 获取状态类型
const getStatusType = (status?: string) => {
  const map: Record<string, any> = { 
    Raw: 'info',
    Indexed: 'info',
    Processing: 'warning',
    Annotated: 'success',
    Verified: 'success',
    Predicted: 'primary',
    Archived: 'info'
  }
  return status ? map[status] || 'info' : 'info'
}

// 获取状态文本
const getStatusText = (status?: string) => {
  const map: Record<string, string> = { 
    Raw: '原始数据',
    Indexed: '已索引',
    Processing: '处理中',
    Annotated: '已标注',
    Verified: '已验证',
    Predicted: '已预测',
    Archived: '已归档'
  }
  return status ? map[status] || status : '-'
}

// 格式化文件大小
const formatFileSize = (bytes?: number) => {
  if (!bytes) return '-'
  const gb = bytes / (1024 * 1024 * 1024)
  if (gb >= 1) return `${gb.toFixed(1)}GB`
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(1)}MB`
}

// 显示导入对话框
const showImportDialog = () => {
  if (!currentProject.value) {
    ElMessage.warning('请先选择一个项目')
    return
  }
  
  if (batches.value.length === 0) {
    ElMessageBox.confirm(
      '当前项目下还没有批次，是否先创建批次？',
      '提示',
      {
        confirmButtonText: '创建批次',
        cancelButtonText: '取消',
        type: 'info'
      }
    ).then(() => {
      showCreateBatchDialog()
    }).catch(() => {})
    return
  }
  
  // 重置表单
  uploadForm.value = {
    batchId: undefined,
    pathologyId: '',
    patientId: ''
  }
  fileList.value = []
  uploadingFiles.value = []
  isUploading.value = false
  
  uploadDialogVisible.value = true
}

// 文件选择变化
const handleFileChange = (file: UploadUserFile, files: UploadUserFile[]) => {
  fileList.value = files
}

// 文件移除
const handleFileRemove = (file: UploadUserFile, files: UploadUserFile[]) => {
  fileList.value = files
}

// 开始上传
const startUpload = async () => {
  if (!uploadForm.value.batchId) {
    ElMessage.warning('请选择所属批次')
    return
  }
  
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  
  isUploading.value = true
  
  try {
    // 逐个上传文件
    for (let i = 0; i < fileList.value.length; i++) {
      const fileItem = fileList.value[i]
      const file = fileItem.raw as File
      
      if (!file) continue
      
      // 添加到上传列表
      uploadingFiles.value.push({
        name: file.name,
        percentage: 0
      })
      
      const uploadIndex = uploadingFiles.value.length - 1
      
      try {
        // 创建分片上传器
        const uploader = new ChunkUploader(file, {
          batchId: uploadForm.value.batchId,
          pathologyId: uploadForm.value.pathologyId || undefined,
          patientId: uploadForm.value.patientId || undefined,
          onProgress: (progress) => {
            uploadingFiles.value[uploadIndex].percentage = progress
          },
          onSuccess: (imageId) => {
            console.log(`文件 ${file.name} 上传成功，图像ID:`, imageId)
            uploadingFiles.value[uploadIndex].status = 'success'
          },
          onError: (error) => {
            console.error(`文件 ${file.name} 上传失败:`, error)
            uploadingFiles.value[uploadIndex].status = 'exception'
            throw error
          },
          onChunkComplete: (chunkIndex, uploaded, total) => {
            console.log(`分片 ${chunkIndex + 1}/${total} 上传完成`)
          },
          onMd5Progress: (progress) => {
            // MD5计算进度，可以显示一个加载状态
            if (progress < 100) {
              uploadingFiles.value[uploadIndex].percentage = Math.round(progress * 0.5) // MD5占50%进度
            }
          }
        })
        
        // 开始上传
        await uploader.start()
        
      } catch (error) {
        console.error(`上传文件 ${file.name} 失败:`, error)
        ElMessage.error(`文件 ${file.name} 上传失败`)
        // 继续上传下一个文件
      }
    }
    
    ElMessage.success('所有文件上传完成')
    
    // 关闭对话框并刷新图像列表
    uploadDialogVisible.value = false
    await loadImages()
    
  } catch (error) {
    console.error('上传过程出错:', error)
    ElMessage.error('上传失败，请重试')
  } finally {
    isUploading.value = false
  }
}

// 批量操作
const handleBatchCommand = (cmd: string) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要操作的图像')
    return
  }
  
  switch (cmd) {
    case 'export':
      ElMessage.success(`导出 ${selectedRows.value.length} 个图像`)
      break
    case 'label':
      ElMessage.success(`分配标注任务给 ${selectedRows.value.length} 个图像`)
      break
    case 'delete':
      ElMessageBox.confirm(
        `确定要删除选中的 ${selectedRows.value.length} 个图像吗？`,
        '警告',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      ).then(() => {
        ElMessage.success('删除成功')
      }).catch(() => {})
      break
  }
}

// 查看图像
const viewImage = (img: Image) => {
  // 跳转到Viewer页面
  router.push({ name: 'Viewer', params: { id: img.imageId } })
}

// 标注图像
const annotateImage = (img: Image) => {
  ElMessage.info(`打开标注编辑器: ${img.originalFilename}`)
  // TODO: 跳转到标注页面
  // router.push({ path: '/annotation', query: { imageId: img.imageId } })
}

// 图片加载错误处理
const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmN2ZhIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzkwOTM5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuaXoOazleWKoOi9veWbvueJhzwvdGV4dD48L3N2Zz4='
}

// 分页变化
const handlePageChange = () => {
  loadImages()
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadImages()
}

// 监听搜索和筛选条件变化
watch([searchText, filterStatus, filterFormat], () => {
  currentPage.value = 1
  loadImages()
})

// 组件挂载时加载数据
onMounted(() => {
  loadProjects()
  loadTags()
})
</script>

<style scoped lang="scss">
.dataset-pool-container {
  height: calc(100vh - 140px);
  
  .main-layout {
    height: 100%;
  }
  
  .nav-card, .content-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    
    :deep(.el-card__body) {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }
  }
  
  .nav-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-actions {
      display: flex;
      gap: 8px;
    }
  }
  
  .project-name-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
  }
  
  .custom-tree-node {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    
    .count-badge {
      color: #909399;
      font-size: 12px;
    }
  }
  
  .tag-filter {
    .filter-title {
      font-weight: 600;
      margin-bottom: 12px;
    }
    .filter-tag {
      margin: 4px;
      cursor: pointer;
    }
  }
  
  .batch-filter {
    margin-bottom: 16px;
    
    .filter-title {
      font-weight: 600;
      margin-bottom: 8px;
    }
  }
  
  .content-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-tools {
      display: flex;
      align-items: center;
    }
  }
  
  .advanced-filter {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #ebeef5;
  }
  
  .view-area {
    min-height: 400px;
  }
  
  .grid-view {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
    
    .grid-item {
      border: 1px solid #e4e7ed;
      border-radius: 8px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s;
      
      &:hover, &.selected {
        border-color: #409EFF;
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
      }
      
      .grid-thumb {
        height: 140px;
        background: #f5f7fa;
        position: relative;
        overflow: hidden;
        
        .thumb-img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        
        .grid-overlay {
          position: absolute;
          top: 8px;
          right: 8px;
        }
      }
      
      .grid-info {
        padding: 12px;
        
        .grid-name {
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          margin-bottom: 4px;
        }
        
        .grid-meta {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
  
  .img-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  // 表格缩略图样式
  .thumbnail-cell {
    display: flex;
    justify-content: center;
    align-items: center;
    
    .table-thumbnail {
      width: 80px;
      height: 80px;
      object-fit: cover;
      border-radius: 4px;
      border: 1px solid #e4e7ed;
      transition: all 0.3s;
      
      &:hover {
        transform: scale(1.1);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        border-color: #409EFF;
      }
    }
  }
  
  .project-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .batch-no {
      font-size: 12px;
      color: #909399;
    }
  }
  
  .pagination-bar {
    margin-top: 20px;
    justify-content: flex-end;
  }
  
  // 上传进度样式
  .upload-progress {
    margin-top: 20px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    max-height: 300px;
    overflow-y: auto;
    
    .progress-item {
      margin-bottom: 16px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .progress-info {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        
        .filename {
          font-size: 14px;
          color: #303133;
          font-weight: 500;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 70%;
        }
        
        .percentage {
          font-size: 14px;
          color: #409EFF;
          font-weight: 600;
        }
      }
    }
  }
}
</style>

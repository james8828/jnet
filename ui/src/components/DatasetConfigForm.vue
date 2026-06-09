<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="140px"
    size="default"
  >
    <!-- 基本信息 -->
    <el-divider content-position="left">基本信息</el-divider>
    
    <el-form-item label="选择项目" prop="projectId">
      <el-select
        v-model="formData.projectId"
        placeholder="请选择项目"
        style="width: 100%"
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
    
    <el-form-item label="任务名称" prop="taskName">
      <el-input v-model="formData.taskName" placeholder="请输入任务名称" />
    </el-form-item>
    
    <el-form-item label="算法类型" prop="algorithmType">
      <el-select
        v-model="formData.algorithmType"
        placeholder="请选择算法类型"
        style="width: 100%"
      >
        <el-option label="YOLO (目标检测)" value="YOLO" />
        <el-option label="COCO (通用格式)" value="COCO" />
        <el-option label="VOC (PASCAL VOC)" value="VOC" />
        <el-option label="SAM (分割任意物体)" value="SAM" />
        <el-option label="CLASSIFICATION (图像分类)" value="CLASSIFICATION" />
      </el-select>
      <div style="margin-top: 4px; color: #909399; font-size: 12px">
        不同算法类型对应不同的数据集格式
      </div>
    </el-form-item>
    
    <el-form-item label="任务描述">
      <el-input
        v-model="formData.description"
        type="textarea"
        :rows="2"
        placeholder="请输入任务描述（可选）"
      />
    </el-form-item>
    
    <el-form-item label="选择批次" v-if="showBatchSelect">
      <el-tree-select
        v-model="formData.batchIds"
        :data="batchTreeData"
        multiple
        show-checkbox
        check-strictly
        placeholder="选择批次（可多选）"
        style="width: 100%"
        :render-after-expand="false"
      >
        <template #default="{ data }">
          <span>{{ data.label }}</span>
          <span v-if="data.imageCount" style="float: right; color: #8492a6; font-size: 13px">
            {{ data.imageCount }} 张
          </span>
        </template>
      </el-tree-select>
      <div style="margin-top: 4px; color: #909399; font-size: 12px">
        已选择 {{ formData.batchIds?.length || 0 }} 个批次
      </div>
    </el-form-item>
    
    <el-form-item label="选择标签">
      <el-select
        v-model="formData.tagIds"
        multiple
        collapse-tags
        collapse-tags-tooltip
        placeholder="选择标签（可多选）"
        style="width: 100%"
      >
        <el-option
          v-for="tag in tags"
          :key="tag.tagId"
          :label="tag.name"
          :value="tag.tagId"
        >
          <span style="display: flex; align-items: center; gap: 8px">
            <span 
              :style="{ 
                display: 'inline-block', 
                width: '12px', 
                height: '12px', 
                borderRadius: '50%', 
                backgroundColor: tag.colorCode || '#409EFF' 
              }"
            />
            {{ tag.name }}
          </span>
        </el-option>
      </el-select>
      <div style="margin-top: 4px; color: #909399; font-size: 12px">
        已选择 {{ formData.tagIds?.length || 0 }} 个标签
      </div>
    </el-form-item>

    <!-- 数据筛选条件 -->
    <el-divider content-position="left">数据筛选条件</el-divider>
    
    <el-form-item label="标注类型">
      <el-select
        v-model="formData.annotationTypes"
        multiple
        placeholder="选择标注类型（不选则包含所有类型）"
        style="width: 100%"
      >
        <el-option label="多边形 (Polygon)" value="polygon" />
        <el-option label="矩形框 (Rectangle)" value="rectangle" />
        <el-option label="点 (Point)" value="point" />
      </el-select>
    </el-form-item>
    
    <el-form-item label="最小标注数量">
      <el-input-number
        v-model="formData.minAnnotationCount"
        :min="0"
        :max="100"
        placeholder="每张图像最少标注数量"
      />
      <span style="margin-left: 10px; color: #909399; font-size: 12px">
        过滤标注数量少于该值的图像
      </span>
    </el-form-item>
    
    <el-form-item label="最大标注数量">
      <el-input-number
        v-model="formData.maxAnnotationCount"
        :min="1"
        :max="1000"
        placeholder="每张图像最多标注数量"
      />
      <span style="margin-left: 10px; color: #909399; font-size: 12px">
        过滤标注数量超过该值的图像
      </span>
    </el-form-item>

    <!-- 数据集配置 -->
    <el-divider content-position="left">数据集配置</el-divider>
    
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="训练集比例">
          <el-input-number
            v-model="formData.trainRatio"
            :min="0.1"
            :max="0.9"
            :step="0.05"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="验证集比例">
          <el-input-number
            v-model="formData.valRatio"
            :min="0.05"
            :max="0.5"
            :step="0.05"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="测试集比例">
          <el-input-number
            v-model="formData.testRatio"
            :min="0"
            :max="0.3"
            :step="0.05"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-alert
      v-if="!isRatioValid"
      title="比例之和必须等于1.0"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 操作按钮 -->
    <el-form-item>
      <el-button type="primary" @click="handleSubmit" :loading="submitting" style="width: 100%">
        <el-icon><VideoPlay /></el-icon>
        {{ submitting ? '创建中...' : '创建任务' }}
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import type { DatasetTaskCreateRequest } from '@/api/dataset-tasks'
import { getBatchesByProject } from '@/api/batches'
import { getAllProjects } from '@/api/projects'
import { getAllTags } from '@/api/tags'

const props = defineProps<{
  projectId?: number
  showBatchSelect?: boolean
}>()

const emit = defineEmits<{
  submit: [data: DatasetTaskCreateRequest]
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const projects = ref<any[]>([])
const batches = ref<any[]>([])
const tags = ref<any[]>([])
const batchTreeData = ref<any[]>([])

const formData = reactive({
  projectId: undefined as number | undefined,
  taskName: '',
  description: '',
  algorithmType: 'YOLO' as string,
  batchIds: [] as number[],
  tagIds: [] as number[],
  trainRatio: 0.7,
  valRatio: 0.2,
  testRatio: 0.1
})

// 验证比例之和是否为1
const isRatioValid = computed(() => {
  const sum = formData.trainRatio + formData.valRatio + formData.testRatio
  return Math.abs(sum - 1.0) < 0.01
})

// 表单验证规则
const rules: FormRules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  algorithmType: [
    { required: true, message: '请选择算法类型', trigger: 'change' }
  ]
}

// 加载项目列表
const loadProjects = async () => {
  try {
    console.log('[DatasetConfigForm] ========== 开始加载项目列表 ==========')
    console.log('[DatasetConfigForm] 调用 getAllProjects API...')
    
    const res = await getAllProjects()
    
    console.log('[DatasetConfigForm] API 响应完整数据:', JSON.stringify(res, null, 2))
    console.log('[DatasetConfigForm] res 类型:', typeof res)
    console.log('[DatasetConfigForm] res 是否为数组:', Array.isArray(res))
    console.log('[DatasetConfigForm] 项目数量:', res?.length || 0)
    
    // getAllProjects 直接返回 ProjectVO[] 数组
    projects.value = Array.isArray(res) ? res : []
    
    console.log('[DatasetConfigForm] 最终项目列表:', projects.value)
    console.log('[DatasetConfigForm] ========== 项目列表加载完成 ==========')
    
    // 如果传入了 projectId，设置默认值
    if (props.projectId) {
      formData.projectId = props.projectId
      console.log('[DatasetConfigForm] 使用传入的 projectId:', props.projectId)
    }
  } catch (error) {
    console.error('[DatasetConfigForm] ❌ 加载项目列表失败:', error)
    console.error('[DatasetConfigForm] 错误详情:', error instanceof Error ? error.message : error)
    ElMessage.error('加载项目列表失败，请查看控制台')
  }
}

// 项目变化时加载批次
const handleProjectChange = async () => {
  if (!formData.projectId) {
    batches.value = []
    batchTreeData.value = []
    formData.batchIds = []
    return
  }
  
  await loadBatches()
}

// 加载批次列表并转换为树形结构
const loadBatches = async () => {
  if (!formData.projectId) return
  
  try {
    const data = await getBatchesByProject(formData.projectId)
    batches.value = data
    
    console.log('[DatasetConfigForm] 批次数据:', data)
    
    // 转换为树形选择器数据格式
    batchTreeData.value = data.map(batch => ({
      value: batch.batchId,  // ← 使用 batchId 而不是 id
      label: batch.batchName,
      imageCount: batch.imageCount || 0
    }))
    
    console.log('[DatasetConfigForm] 批次树形数据:', batchTreeData.value)
  } catch (error) {
    console.error('[DatasetConfigForm] 加载批次列表失败:', error)
  }
}

// 加载标签列表（全量，不需要项目ID）
const loadTags = async () => {
  try {
    console.log('[DatasetConfigForm] 开始加载所有标签...')
    const data = await getAllTags()
    tags.value = Array.isArray(data) ? data : []
    console.log('[DatasetConfigForm] 标签数量:', tags.value.length)
  } catch (error) {
    console.error('[DatasetConfigForm] 加载标签列表失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  // 验证表单
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表单信息')
      return
    }
    
    // 验证比例
    if (!isRatioValid.value) {
      ElMessage.error('训练/验证/测试集比例之和必须等于1.0')
      return
    }
    
    // 构建请求数据（与后端 DatasetBuildRequestDTO 对齐）
    const requestData: DatasetTaskCreateRequest = {
      projectId: formData.projectId!,
      taskName: formData.taskName || undefined,
      description: formData.description || undefined,
      algorithmType: formData.algorithmType,
      batchIds: formData.batchIds.length > 0 ? formData.batchIds : undefined,
      tagIds: formData.tagIds.length > 0 ? formData.tagIds : undefined,
      trainRatio: formData.trainRatio,
      valRatio: formData.valRatio,
      testRatio: formData.testRatio
    }
    
    console.log('[DatasetConfigForm] 提交的数据:', JSON.stringify(requestData, null, 2))
    
    submitting.value = true
    try {
      emit('submit', requestData)
    } catch (error) {
      ElMessage.error('创建任务失败')
    } finally {
      submitting.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  formData.batchIds = []
  formData.tagIds = []
  // 重置为默认值
  formData.algorithmType = 'YOLO'
  formData.trainRatio = 0.7
  formData.valRatio = 0.2
  formData.testRatio = 0.1
}

onMounted(() => {
  loadProjects()
  loadTags()  // 页面加载时就获取所有标签
})

// 暴露方法给父组件
defineExpose({
  resetForm
})
</script>

<style scoped lang="scss">
</style>

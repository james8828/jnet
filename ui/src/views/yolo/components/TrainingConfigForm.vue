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
    
    <el-form-item label="任务名称" prop="taskName">
      <el-input v-model="formData.taskName" placeholder="请输入任务名称" />
    </el-form-item>
    
    <el-form-item label="任务描述">
      <el-input
        v-model="formData.description"
        type="textarea"
        :rows="2"
        placeholder="请输入任务描述（可选）"
      />
    </el-form-item>

    <!-- 数据源配置 -->
    <el-divider content-position="left">数据源配置</el-divider>
    
    <el-form-item label="数据集任务">
      <el-select
        v-model="formData.datasetTaskId"
        placeholder="选择已构建的数据集（可选）"
        clearable
        style="width: 100%"
      >
        <el-option
          v-for="ds in datasetTasks"
          :key="ds.taskId"
          :label="ds.taskName"
          :value="ds.taskId"
        >
          <span>{{ ds.taskName }}</span>
          <span style="float: right; color: #8492a6; font-size: 13px">
            {{ ds.totalImages || 0 }} 张
          </span>
        </el-option>
      </el-select>
    </el-form-item>
    
    <el-form-item label="自定义数据集路径">
      <el-input
        v-model="formData.customDatasetPath"
        placeholder="或输入自定义数据集路径（可选）"
        clearable
      />
      <div style="margin-top: 4px; color: #909399; font-size: 12px">
        如果未选择数据集任务，可在此指定已有数据集的路径
      </div>
    </el-form-item>

    <!-- 模型配置 -->
    <el-divider content-position="left">模型配置</el-divider>
    
    <el-form-item label="模型架构" prop="modelArchitecture">
      <el-select v-model="formData.modelArchitecture" style="width: 100%">
        <el-option label="YOLOv8n (nano - 最快)" value="yolov8n" />
        <el-option label="YOLOv8s (small - 快速)" value="yolov8s" />
        <el-option label="YOLOv8m (medium - 平衡)" value="yolov8m" />
        <el-option label="YOLOv8l (large - 准确)" value="yolov8l" />
        <el-option label="YOLOv8x (extra large - 最准确)" value="yolov8x" />
      </el-select>
    </el-form-item>
    
    <el-form-item label="预训练权重">
      <el-select v-model="formData.pretrainedWeights" style="width: 100%">
        <el-option label="COCO数据集权重" value="coco" />
        <el-option label="ImageNet数据集权重" value="imagenet" />
        <el-option label="不使用预训练权重" value="" />
      </el-select>
    </el-form-item>

    <!-- 训练超参数 -->
    <el-divider content-position="left">训练超参数</el-divider>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="训练轮数 (Epochs)">
          <el-input-number
            v-model="formData.epochs"
            :min="10"
            :max="1000"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="批次大小 (Batch Size)">
          <el-input-number
            v-model="formData.batchSize"
            :min="1"
            :max="128"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="图像尺寸">
          <el-input-number
            v-model="formData.imageSize"
            :min="320"
            :max="1280"
            :step="32"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="学习率">
          <el-input-number
            v-model="formData.learningRate"
            :min="0.0001"
            :max="0.1"
            :step="0.0001"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="优化器">
          <el-select v-model="formData.optimizer" style="width: 100%">
            <el-option label="SGD" value="SGD" />
            <el-option label="Adam" value="Adam" />
            <el-option label="AdamW" value="AdamW" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="学习率调度器">
          <el-select v-model="formData.lrScheduler" style="width: 100%">
            <el-option label="Cosine" value="cosine" />
            <el-option label="Linear" value="linear" />
            <el-option label="StepLR" value="step" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="动量 (Momentum)">
          <el-input-number
            v-model="formData.momentum"
            :min="0"
            :max="1"
            :step="0.001"
            :precision="3"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="权重衰减 (Weight Decay)">
          <el-input-number
            v-model="formData.weightDecay"
            :min="0"
            :max="0.01"
            :step="0.0001"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="预热轮数">
          <el-input-number
            v-model="formData.warmupEpochs"
            :min="0"
            :max="20"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="早停耐心值">
          <el-input-number
            v-model="formData.patience"
            :min="10"
            :max="200"
            :step="10"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 数据增强 -->
    <el-divider content-position="left">数据增强</el-divider>
    
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="HSV色调">
          <el-slider
            v-model="formData.hsvH"
            :min="0"
            :max="0.1"
            :step="0.001"
            :precision="3"
            show-input
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="HSV饱和度">
          <el-slider
            v-model="formData.hsvS"
            :min="0"
            :max="1"
            :step="0.01"
            :precision="2"
            show-input
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="HSV亮度">
          <el-slider
            v-model="formData.hsvV"
            :min="0"
            :max="1"
            :step="0.01"
            :precision="2"
            show-input
          />
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="旋转角度">
          <el-slider
            v-model="formData.degrees"
            :min="0"
            :max="45"
            :step="1"
            show-input
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="平移">
          <el-slider
            v-model="formData.translate"
            :min="0"
            :max="0.5"
            :step="0.01"
            :precision="2"
            show-input
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="缩放">
          <el-slider
            v-model="formData.scale"
            :min="0"
            :max="1"
            :step="0.01"
            :precision="2"
            show-input
          />
        </el-form-item>
      </el-col>
    </el-row>
    
    <el-form-item label="翻转增强">
      <el-checkbox v-model="formData.flipLr">水平翻转</el-checkbox>
      <el-checkbox v-model="formData.flipUd" style="margin-left: 16px">垂直翻转</el-checkbox>
    </el-form-item>

    <!-- 硬件配置 -->
    <el-divider content-position="left">硬件配置</el-divider>
    
    <el-form-item label="GPU设备ID">
      <el-input
        v-model="formData.gpuIds"
        placeholder="例如: 0,1,2 或 cpu"
      />
      <div style="margin-top: 4px; color: #909399; font-size: 12px">
        多个GPU用逗号分隔，使用CPU则填写"cpu"
      </div>
    </el-form-item>
    
    <el-form-item label="数据加载线程数">
      <el-input-number
        v-model="formData.numWorkers"
        :min="0"
        :max="16"
        :step="1"
      />
    </el-form-item>
    
    <el-form-item label="混合精度训练">
      <el-switch v-model="formData.mixedPrecision" />
      <span style="margin-left: 10px; color: #909399; font-size: 12px">
        启用FP16混合精度可加速训练并减少显存占用
      </span>
    </el-form-item>

    <!-- 操作按钮 -->
    <el-form-item>
      <el-button type="primary" @click="handleSubmit" :loading="submitting" style="width: 100%">
        <el-icon><VideoPlay /></el-icon>
        {{ submitting ? '创建中...' : '开始训练' }}
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import type { TrainingTaskCreateRequest } from '@/api/training-tasks'
import { listDatasetTasks, type DatasetTask } from '@/api/dataset-tasks'

const props = defineProps<{
  projectId: number
}>()

const emit = defineEmits<{
  submit: [data: TrainingTaskCreateRequest]
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const datasetTasks = ref<DatasetTask[]>([])

const formData = reactive({
  taskName: '',
  description: '',
  datasetTaskId: undefined as number | undefined,
  customDatasetPath: '',
  modelArchitecture: 'yolov8n',
  pretrainedWeights: 'coco',
  epochs: 100,
  batchSize: 16,
  imageSize: 640,
  learningRate: 0.01,
  momentum: 0.937,
  weightDecay: 0.0005,
  optimizer: 'SGD',
  lrScheduler: 'cosine',
  warmupEpochs: 3,
  patience: 50,
  hsvH: 0.015,
  hsvS: 0.7,
  hsvV: 0.4,
  degrees: 0.0,
  translate: 0.1,
  scale: 0.5,
  flipLr: true,
  flipUd: false,
  gpuIds: '0',
  numWorkers: 4,
  mixedPrecision: true
})

// 表单验证规则
const rules: FormRules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  modelArchitecture: [
    { required: true, message: '请选择模型架构', trigger: 'change' }
  ]
}

// 加载可用的数据集任务
const loadDatasetTasks = async () => {
  try {
    const res = await listDatasetTasks({
      projectId: props.projectId,
      status: 'SUCCESS',
      current: 1,
      size: 100
    })
    datasetTasks.value = res.records
  } catch (error) {
    console.error('加载数据集任务失败:', error)
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
    
    // 构建请求数据
    const requestData: TrainingTaskCreateRequest = {
      projectId: props.projectId,
      taskName: formData.taskName,
      description: formData.description || undefined,
      datasetTaskId: formData.datasetTaskId,
      customDatasetPath: formData.customDatasetPath || undefined,
      modelArchitecture: formData.modelArchitecture,
      pretrainedWeights: formData.pretrainedWeights || undefined,
      epochs: formData.epochs,
      batchSize: formData.batchSize,
      imageSize: formData.imageSize,
      learningRate: formData.learningRate,
      momentum: formData.momentum,
      weightDecay: formData.weightDecay,
      optimizer: formData.optimizer,
      lrScheduler: formData.lrScheduler,
      warmupEpochs: formData.warmupEpochs,
      patience: formData.patience,
      augmentationConfig: {
        hsvH: formData.hsvH,
        hsvS: formData.hsvS,
        hsvV: formData.hsvV,
        degrees: formData.degrees,
        translate: formData.translate,
        scale: formData.scale,
        flipLr: formData.flipLr,
        flipUd: formData.flipUd
      },
      gpuIds: formData.gpuIds,
      numWorkers: formData.numWorkers,
      mixedPrecision: formData.mixedPrecision
    }
    
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
}

onMounted(() => {
  loadDatasetTasks()
})

// 暴露方法给父组件
defineExpose({
  resetForm
})
</script>

<style scoped lang="scss">
:deep(.el-slider) {
  margin-right: 20px;
}
</style>

/**
 * 模型训练任务 API 接口
 * 支持 YOLO 等算法的模型训练管理
 */
import request, { PageData } from '@/utils/request'
import { createApiPath, SERVICES } from '@/config/services'

const BASE_URL = createApiPath(SERVICES.BIZ, '/api/v1/yolo/training-tasks')

// ==================== 类型定义 ====================

/**
 * 模型训练任务
 */
export interface TrainingTask {
  taskId: number
  taskNo: string
  projectId: number
  taskName: string
  description?: string
  datasetTaskId?: number
  datasetPath?: string
  modelArchitecture: string
  pretrainedWeights?: string
  epochs: number
  batchSize: number
  imageSize: number
  learningRate: number
  momentum?: number
  weightDecay?: number
  optimizer?: string
  lrScheduler?: string
  warmupEpochs?: number
  patience?: number
  additionalParams?: any
  augmentationConfig?: any
  gpuIds?: string
  numWorkers?: number
  mixedPrecision?: boolean
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
  progress: number
  currentEpoch?: number
  currentStep?: string
  metricsJson?: any
  bestMetrics?: any
  modelId?: number
  modelPath?: string
  bestModelPath?: string
  evaluationResults?: any
  errorMessage?: string
  errorStack?: string
  createTime: string
  startTime?: string
  endTime?: string
  durationSeconds?: number
}

/**
 * 创建训练任务请求参数
 */
export interface TrainingTaskCreateRequest {
  projectId: number
  taskName: string
  description?: string
  datasetTaskId?: number
  datasetPath?: string
  customDatasetPath?: string
  modelArchitecture?: string
  pretrainedWeights?: string
  epochs?: number
  batchSize?: number
  imageSize?: number
  learningRate?: number
  momentum?: number
  weightDecay?: number
  optimizer?: string
  lrScheduler?: string
  warmupEpochs?: number
  patience?: number
  additionalParams?: any
  augmentationConfig?: any
  gpuIds?: string
  numWorkers?: number
  mixedPrecision?: boolean
}

/**
 * 训练任务查询参数
 */
export interface TrainingTaskQueryParams {
  projectId?: number
  status?: string
  taskName?: string
  modelArchitecture?: string
  datasetTaskId?: number
  current?: number
  size?: number
}

// ==================== API 方法 ====================

/**
 * 创建模型训练任务
 */
export function createTrainingTask(data: TrainingTaskCreateRequest) {
  return request.post<{ taskId: number; taskNo: string }>(`${BASE_URL}/training/tasks`, data)
}

/**
 * 分页查询训练任务列表
 */
export function listTrainingTasks(data: TrainingTaskQueryParams) {
  return request.post<PageData<TrainingTask>>(`${BASE_URL}/page`, data)
}

/**
 * 查询训练任务详情
 */
export function getTrainingTask(taskId: number) {
  return request.get<TrainingTask>(`${BASE_URL}/training/tasks/${taskId}`)
}

/**
 * 取消训练任务
 */
export function cancelTrainingTask(taskId: number) {
  return request.post(`${BASE_URL}/training/tasks/${taskId}/cancel`)
}

/**
 * 下载训练好的模型
 */
export function downloadModel(taskId: number) {
  return request.get(`${BASE_URL}/training/tasks/${taskId}/model/download`, {
    responseType: 'blob'
  })
}

/**
 * 删除训练任务
 */
export function deleteTrainingTask(taskId: number) {
  return request.delete(`${BASE_URL}/training/tasks/${taskId}`)
}

/**
 * 获取训练指标历史
 */
export function getTrainingMetrics(taskId: number) {
  return request.get<any[]>(`${BASE_URL}/training/tasks/${taskId}/metrics`)
}

/**
 * 评估模型
 */
export function evaluateModel(taskId: number) {
  return request.post(`${BASE_URL}/training/tasks/${taskId}/evaluate`)
}

/**
 * 导出模型（ONNX/TensorRT/OpenVINO等格式）
 */
export function exportModel(taskId: number, format: 'onnx' | 'tensorrt' | 'openvino') {
  return request.post(`${BASE_URL}/training/tasks/${taskId}/export`, { format })
}

/**
 * 数据集构建任务 API 接口
 * 支持 YOLO、COCO、VOC、SAM 等多种算法的数据集构建
 */
import request, { PageData } from '@/utils/request'
import { createApiPath, SERVICES } from '@/config/services'

const BASE_URL = createApiPath(SERVICES.BIZ, '/api/v1/dataset-build-tasks')

// ==================== 类型定义 ====================

/**
 * 数据集构建任务
 */
export interface DatasetTask {
  taskId: number
  taskNo: string
  projectId: number
  batchIds?: string      // JSON 字符串，如 "[3]"
  tagIds?: string        // JSON 字符串，如 "[1, 3]"
  algorithmType: string  // YOLO, COCO, VOC, SAM, CLASSIFICATION
  taskName: string
  description?: string
  trainRatio?: number
  valRatio?: number
  testRatio?: number
  classMapping?: any     // JSON 对象
  shuffle?: boolean
  outputFormat?: string  // yolov5, yolov8, coco, voc
  includeImages?: boolean
  compressFormat?: string // zip, tar.gz, none
  compressQuality?: number
  minImageSize?: number
  maxImageSize?: number
  extraConfig?: string   // JSON 格式的额外配置
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
  progress: number
  currentStep?: string
  stepDetail?: any
  totalImages?: number
  totalAnnotations?: number
  trainCount?: number
  valCount?: number
  testCount?: number
  classDistribution?: string  // JSON 字符串
  datasetPath?: string
  datasetSize?: number
  dataYamlPath?: string
  errorMessage?: string
  errorStack?: string
  createBy?: number
  createTime: string
  startTime?: string
  endTime?: string
  durationSeconds?: number
  updateBy?: number
  updateTime?: string
}

/**
 * 创建数据集任务请求参数
 */
export interface DatasetTaskCreateRequest {
  projectId: number
  taskName?: string
  description?: string
  algorithmType: string  // 必填：YOLO, COCO, VOC, SAM, CLASSIFICATION
  batchIds?: number[]
  tagIds?: number[]      // 标签ID列表
  trainRatio?: number
  valRatio?: number
  testRatio?: number
  minImageSize?: number
  maxImageSize?: number
  compress?: boolean
  compressQuality?: number
  outputFormat?: 'yolov5' | 'yolov8' | 'coco' | 'voc'
  extraConfig?: string   // JSON格式的额外配置
}

/**
 * 数据集任务查询参数
 */
export interface DatasetTaskQueryParams {
  projectId?: number
  taskName?: string
  status?: string
  currentStep?: string
  algorithmType?: string
  current?: number
  size?: number
}

// ==================== API 方法 ====================

/**
 * 创建数据集构建任务
 */
export function createDatasetTask(data: DatasetTaskCreateRequest) {
  return request.post<{ taskId: number; taskNo: string }>(`${BASE_URL}/build`, data)
}

/**
 * 分页查询数据集任务列表
 */
export function listDatasetTasks(data: DatasetTaskQueryParams) {
  return request.post<PageData<DatasetTask>>(`${BASE_URL}/page`, data)
}

/**
 * 查询数据集任务详情
 */
export function getDatasetTask(taskId: number) {
  return request.get<DatasetTask>(`${BASE_URL}/${taskId}`)
}

/**
 * 取消数据集任务
 */
export function cancelDatasetTask(taskId: number) {
  return request.post(`${BASE_URL}/${taskId}/cancel`)
}

/**
 * 下载数据集
 */
export function downloadDataset(taskId: number) {
  return request.get(`${BASE_URL}/${taskId}/download`, {
    responseType: 'blob'
  })
}

/**
 * 删除数据集任务
 */
export function deleteDatasetTask(taskId: number) {
  return request.delete(`${BASE_URL}/${taskId}`)
}

/**
 * 重新触发训练（如果配置了自动触发）
 */
export function triggerTrainingFromDataset(taskId: number) {
  return request.post(`${BASE_URL}/dataset/tasks/${taskId}/trigger-training`)
}

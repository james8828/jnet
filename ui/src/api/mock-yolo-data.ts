/**
 * YOLO API Mock数据
 * 用于前端独立开发和测试
 * 
 * 使用方法：
 * 1. 在开发环境中替换真实API调用
 * 2. 或在后端未就绪时进行前端功能验证
 */

import type { YoloDatasetTask, YoloTrainingTask } from '@/api/yolo'
import type { PageData } from '@/utils/request'

// ==================== Mock数据集任务 ====================

export const mockDatasetTasks: YoloDatasetTask[] = [
  {
    taskId: 1,
    taskNo: 'DS-2026051101',
    projectId: 1,
    batchId: 1,
    taskName: '病理切片细胞检测数据集v1',
    description: '用于细胞检测的YOLO训练数据集',
    status: 'SUCCESS',
    progress: 100,
    currentStep: '完成',
    totalImages: 1248,
    totalAnnotations: 3567,
    trainCount: 998,
    valCount: 125,
    testCount: 125,
    classDistribution: {
      'cell': 2456,
      'nucleus': 1111
    },
    datasetPath: '/datasets/ds-1.zip',
    datasetSize: 524288000, // 500MB
    dataYamlPath: '/datasets/ds-1/data.yaml',
    createTime: '2026-05-10T10:30:00',
    startTime: '2026-05-10T10:30:15',
    endTime: '2026-05-10T10:45:30',
    durationSeconds: 915
  },
  {
    taskId: 2,
    taskNo: 'DS-2026051102',
    projectId: 1,
    taskName: 'WSI组织区域检测数据集',
    status: 'RUNNING',
    progress: 65.5,
    currentStep: '复制图像文件',
    stepDetail: {
      processed: 650,
      total: 1000
    },
    totalImages: 1000,
    createTime: '2026-05-11T09:15:00',
    startTime: '2026-05-11T09:15:20'
  },
  {
    taskId: 3,
    taskNo: 'DS-2026051103',
    projectId: 1,
    taskName: '肿瘤区域标注数据集',
    status: 'FAILED',
    progress: 30.2,
    currentStep: '生成YOLO格式标注文件',
    errorMessage: '图像文件读取失败: image_0456.tif',
    errorStack: 'java.io.IOException: Unsupported image format\n\tat com.jnet...',
    createTime: '2026-05-11T08:00:00',
    startTime: '2026-05-11T08:00:10',
    endTime: '2026-05-11T08:05:45',
    durationSeconds: 335
  },
  {
    taskId: 4,
    taskNo: 'DS-2026051104',
    projectId: 1,
    taskName: '血管分割数据集',
    status: 'PENDING',
    progress: 0,
    currentStep: '等待执行',
    createTime: '2026-05-11T14:20:00'
  }
]

// ==================== Mock训练任务 ====================

export const mockTrainingTasks: YoloTrainingTask[] = [
  {
    taskId: 1,
    taskNo: 'TR-2026051001',
    projectId: 1,
    taskName: '细胞检测模型训练-yolov8n',
    description: '使用YOLOv8n进行细胞检测模型训练',
    datasetTaskId: 1,
    modelArchitecture: 'yolov8n',
    pretrainedWeights: 'coco',
    epochs: 100,
    batchSize: 16,
    imageSize: 640,
    learningRate: 0.01,
    status: 'SUCCESS',
    progress: 100,
    currentEpoch: 100,
    currentStep: '训练完成',
    metricsJson: {
      epoch: 100,
      loss: 0.234,
      map50: 0.923,
      precision: 0.915,
      recall: 0.898
    },
    bestMetrics: {
      map50: 0.935,
      map50_95: 0.678,
      precision: 0.925,
      recall: 0.910,
      f1Score: 0.917
    },
    modelPath: '/models/tr-1/final.pt',
    bestModelPath: '/models/tr-1/best.pt',
    modelSize: 12582912, // 12MB
    evaluationResults: {
      map50: 0.935,
      map50_95: 0.678,
      precision: 0.925,
      recall: 0.910
    },
    createTime: '2026-05-10T11:00:00',
    startTime: '2026-05-10T11:00:30',
    endTime: '2026-05-10T13:15:45',
    durationSeconds: 8115
  },
  {
    taskId: 2,
    taskNo: 'TR-2026051101',
    projectId: 1,
    taskName: '组织区域检测-yolov8m',
    datasetTaskId: 2,
    modelArchitecture: 'yolov8m',
    pretrainedWeights: 'coco',
    epochs: 150,
    batchSize: 8,
    imageSize: 640,
    learningRate: 0.01,
    status: 'RUNNING',
    progress: 45.3,
    currentEpoch: 68,
    currentStep: 'Epoch 68/150',
    metricsJson: {
      epoch: 68,
      loss: 0.456,
      map50: 0.856,
      precision: 0.845,
      recall: 0.832
    },
    bestMetrics: {
      map50: 0.878,
      map50_95: 0.623,
      precision: 0.867,
      recall: 0.854
    },
    createTime: '2026-05-11T09:30:00',
    startTime: '2026-05-11T09:30:45'
  },
  {
    taskId: 3,
    taskNo: 'TR-2026051102',
    projectId: 1,
    taskName: '肿瘤检测模型训练',
    modelArchitecture: 'yolov8l',
    pretrainedWeights: 'coco',
    epochs: 200,
    batchSize: 4,
    imageSize: 640,
    learningRate: 0.005,
    status: 'FAILED',
    progress: 15.5,
    currentEpoch: 31,
    currentStep: '训练失败',
    errorMessage: 'CUDA out of memory. Tried to allocate 2.5 GiB',
    createTime: '2026-05-11T08:30:00',
    startTime: '2026-05-11T08:30:20',
    endTime: '2026-05-11T08:45:10',
    durationSeconds: 890
  }
]

// ==================== Mock API函数 ====================

/**
 * 模拟延迟
 */
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

/**
 * 获取数据集任务列表（Mock）
 */
export async function mockListDatasetTasks(params: {
  projectId: number
  status?: string
  pageNum: number
  pageSize: number
}): Promise<PageData<YoloDatasetTask>> {
  await delay(500) // 模拟网络延迟
  
  let filtered = mockDatasetTasks.filter(t => t.projectId === params.projectId)
  
  if (params.status) {
    filtered = filtered.filter(t => t.status === params.status)
  }
  
  const start = (params.pageNum - 1) * params.pageSize
  const end = start + params.pageSize
  const list = filtered.slice(start, end)
  
  return {
    total: filtered.length,
    list
  }
}

/**
 * 获取训练任务列表（Mock）
 */
export async function mockListTrainingTasks(params: {
  projectId: number
  status?: string
  modelArchitecture?: string
  pageNum: number
  pageSize: number
}): Promise<PageData<YoloTrainingTask>> {
  await delay(500)
  
  let filtered = mockTrainingTasks.filter(t => t.projectId === params.projectId)
  
  if (params.status) {
    filtered = filtered.filter(t => t.status === params.status)
  }
  
  if (params.modelArchitecture) {
    filtered = filtered.filter(t => t.modelArchitecture === params.modelArchitecture)
  }
  
  const start = (params.pageNum - 1) * params.pageSize
  const end = start + params.pageSize
  const list = filtered.slice(start, end)
  
  return {
    total: filtered.length,
    list
  }
}

/**
 * 创建数据集任务（Mock）
 */
export async function mockCreateDatasetTask(data: any) {
  await delay(800)
  
  const newTask: YoloDatasetTask = {
    taskId: mockDatasetTasks.length + 1,
    taskNo: `DS-${Date.now()}`,
    projectId: data.projectId,
    taskName: data.taskName,
    description: data.description,
    status: 'PENDING',
    progress: 0,
    currentStep: '等待执行',
    createTime: new Date().toISOString()
  }
  
  mockDatasetTasks.unshift(newTask)
  
  return {
    taskId: newTask.taskId,
    taskNo: newTask.taskNo
  }
}

/**
 * 创建训练任务（Mock）
 */
export async function mockCreateTrainingTask(data: any) {
  await delay(800)
  
  const newTask: YoloTrainingTask = {
    taskId: mockTrainingTasks.length + 1,
    taskNo: `TR-${Date.now()}`,
    projectId: data.projectId,
    taskName: data.taskName,
    description: data.description,
    modelArchitecture: data.modelArchitecture,
    epochs: data.epochs || 100,
    batchSize: data.batchSize || 16,
    imageSize: data.imageSize || 640,
    learningRate: data.learningRate || 0.01,
    status: 'PENDING',
    progress: 0,
    currentStep: '等待执行',
    createTime: new Date().toISOString()
  }
  
  mockTrainingTasks.unshift(newTask)
  
  return {
    taskId: newTask.taskId,
    taskNo: newTask.taskNo
  }
}

/**
 * 模拟WebSocket进度推送
 */
export function simulateProgressUpdate(taskId: number, callback: (data: any) => void) {
  let progress = 0
  const interval = setInterval(() => {
    progress += Math.random() * 10
    if (progress >= 100) {
      progress = 100
      clearInterval(interval)
      
      callback({
        taskId,
        progress: 100,
        currentStep: '完成',
        status: 'SUCCESS',
        updateTime: new Date().toISOString()
      })
    } else {
      callback({
        taskId,
        progress: Math.min(progress, 99.9),
        currentStep: `处理中... ${Math.floor(progress)}%`,
        status: 'RUNNING',
        updateTime: new Date().toISOString()
      })
    }
  }, 1000)
  
  // 返回清理函数
  return () => clearInterval(interval)
}

// ==================== 使用示例 ====================

/*
// 在开发环境中替换真实API
import { mockListDatasetTasks } from './mock-yolo-data'

// 临时替换
const originalListDatasetTasks = listDatasetTasks
listDatasetTasks = mockListDatasetTasks as any
*/

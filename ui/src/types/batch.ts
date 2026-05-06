/**
 * 批次相关类型定义
 */

// 批次实体
export interface Batch {
  batchId: number
  projectId: number
  batchCode: string
  batchName?: string
  scannerModel?: string
  stainingProtocol?: string
  storageRootPath?: string
  totalImages?: number
  uploadStatus?: string  // pending/uploading/completed/failed
  createBy?: number
  createTime: string
  updateBy?: number
  updateTime: string
}

// 批次VO
export interface BatchVO extends Batch {
  projectName?: string
}

// 批次DTO
export interface BatchDTO {
  projectId: number
  batchCode: string
  batchName?: string
  scannerModel?: string
  stainingProtocol?: string
  storageRootPath?: string
  uploadStatus?: string  // pending/uploading/completed/failed
}

// 批次查询DTO
export interface BatchQueryDTO {
  current: number
  size: number
  projectId?: number
  batchCode?: string
  batchName?: string
  scannerModel?: string
  uploadStatus?: string  // pending/uploading/completed/failed
  startDate?: string
  endDate?: string
}

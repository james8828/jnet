/**
 * 图像相关类型定义
 */

// 图像格式枚举
export enum ImageFormat {
  SVS = 'SVS',
  TIFF = 'TIFF',
  JPG = 'JPG',
  PNG = 'PNG',
  NDPI = 'NDPI'
}

// 图像实体
export interface Image {
  imageId: number
  filename: string
  originalFilename: string
  batchId: number
  projectId?: number
  pathologyId?: string
  patientId?: string
  format?: string  // SVS/TIFF/JPG/PNG/NDPI
  fileSize?: number
  width?: number
  height?: number
  levels?: number
  lifecycleStatus?: string  // Raw/Indexed/Processing/Annotated/Verified/Predicted/Archived
  annotationProgress?: number // 0-100
  metadata?: string // JSON字符串
  storagePath: string
  thumbnailUrl?: string  // 缩略图URL（数据库字段：thumbnail_url）
  md5?: string
  createBy?: number
  createTime: string
  updateBy?: number
  updateTime: string
}

// 图像元数据VO
export interface ImageMetadataVO {
  imageId: number
  width: number
  height: number
  levels: number
  levelDimensions: Array<{
    level: number
    width: number
    height: number
  }>
  tileWidth: number
  tileHeight: number
  format: string
  magnification?: number
  resolution?: {
    x: number
    y: number
    unit: string
  }
}

// 图像查询DTO
export interface ImageQueryDTO {
  current: number
  size: number
  batchId?: number
  projectId?: number
  pathologyId?: string
  patientId?: string
  lifecycleStatus?: string  // Raw/Indexed/Processing/Annotated/Verified/Predicted/Archived
  format?: string  // SVS/TIFF/JPG/PNG/NDPI
  keyword?: string // 模糊搜索文件名
  startDate?: string
  endDate?: string
}

// 图像状态更新DTO
export interface ImageStatusDTO {
  status: string  // Raw/Indexed/Processing/Annotated/Verified/Predicted/Archived
}

// 标注进度更新DTO
export interface AnnotationProgressDTO {
  imageIds: number[]
  progress: number
}

// 分片上传初始化DTO
export interface ChunkUploadInitDTO {
  fileMd5: string
  filename: string
  fileSize: number
  chunkSize: number
  batchId: number
  pathologyId?: string
  patientId?: string
}

// 分片上传VO
export interface ChunkUploadVO {
  uploadId: string
  fileMd5: string
  chunkSize: number
  totalChunks: number
  uploadedChunks: number[]
  needUpload: boolean // 是否需要上传（秒传检测）
}

// 分片上传DTO
export interface ChunkUploadDTO {
  fileMd5: string
  chunkIndex: number
  chunk: File
}

// Tile查询DTO
export interface TileQueryDTO {
  imageId: number
  level: number
  row: number
  col: number
}

// 批量选择图像DTO
export interface BatchSelectImagesDTO {
  sourceType: 'IMAGE_STORE' | 'EXISTING_BATCH' // 来源类型
  sourceBatchId?: number // 源批次ID（当sourceType为EXISTING_BATCH时）
  imageIds: number[] // 选择的图像ID列表
  targetBatchId: number // 目标批次ID
}

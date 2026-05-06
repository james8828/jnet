/**
 * 图像管理API接口
 */
import request, { PageData } from '@/utils/request'
import { createApiPath, SERVICES } from '@/config/services'
import type {
  Image,
  ImageQueryDTO,
  ImageStatusDTO,
  AnnotationProgressDTO,
  ChunkUploadInitDTO,
  ChunkUploadVO,
  ChunkUploadDTO,
  TileQueryDTO,
  ImageMetadataVO,
  BatchSelectImagesDTO
} from '@/types/image'

const BASE_URL = createApiPath(SERVICES.BIZ, '/api/v1/images')

// 调试日志：确认 BASE_URL 正确
if (import.meta.env.DEV) {
  console.log('[Images API] BASE_URL:', BASE_URL)
}

/**
 * 高级检索图像（分页）
 */
export function searchImages(query: ImageQueryDTO) {
  return request.post<PageData<Image>>(`${BASE_URL}/search`, query)
}

/**
 * 分页查询图像列表
 */
export function getImagePage(query: ImageQueryDTO) {
  return request.post<PageData<Image>>(`${BASE_URL}/page`, query)
}

/**
 * 获取图像详情
 */
export function getImageById(id: number) {
  return request.get<Image>(`${BASE_URL}/${id}`)
}

/**
 * 更新图像生命周期状态
 */
export function updateImageStatus(id: number, data: ImageStatusDTO) {
  return request.put(`${BASE_URL}/${id}/status`, data)
}

/**
 * 批量更新标注进度
 */
export function updateAnnotationProgress(data: AnnotationProgressDTO) {
  return request.put(`${BASE_URL}/annotation-progress`, data)
}

// ==================== 分片上传相关接口 ====================

/**
 * 初始化分片上传
 */
export function initChunkUpload(data: ChunkUploadInitDTO) {
  return request.post<ChunkUploadVO>(`${BASE_URL}/chunk/init`, data)
}

/**
 * 上传分片
 */
export function uploadChunk(data: ChunkUploadDTO) {
  const formData = new FormData()
  formData.append('fileMd5', data.fileMd5)
  formData.append('chunkIndex', String(data.chunkIndex))
  formData.append('file', data.chunk)  // 修改为 'file' 以匹配后端DTO
  
  return request.post<boolean>(`${BASE_URL}/chunk/upload`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 合并分片
 */
export function mergeChunks(
  fileMd5: string,
  batchId: number,
  filename: string,
  pathologyId?: string,
  patientId?: string
) {
  return request.post<number>(`${BASE_URL}/chunk/merge`, null, {
    params: {
      fileMd5,
      batchId,
      filename,
      pathologyId,
      patientId
    }
  })
}

/**
 * 取消上传
 */
export function cancelUpload(fileMd5: string) {
  return request.delete(`${BASE_URL}/chunk/cancel`, {
    params: { fileMd5 }
  })
}

/**
 * 批量选择切片
 */
export function batchSelectImages(data: BatchSelectImagesDTO) {
  return request.post(`${BASE_URL}/batch-select`, data)
}

// ==================== 缩略图和瓦片相关接口 ====================

/**
 * 获取图像元数据
 */
export function getImageMetadata(id: number) {
  return request.get<ImageMetadataVO>(`${BASE_URL}/${id}/metadata`)
}

/**
 * 获取缩略图URL
 */
export function getThumbnailUrl(id: number, maxSize: number = 512): string {
  const queryString = new URLSearchParams({
    maxSize: String(maxSize)
  }).toString()
  const url = `${BASE_URL}/${id}/thumbnail?${queryString}`
  console.log('[getThumbnailUrl] imageId:', id, 'maxSize:', maxSize, 'URL:', url)
  return url
}

/**
 * 获取Tile瓦片URL
 */
export function getTileUrl(params: TileQueryDTO): string {
  const queryString = new URLSearchParams({
    imageId: String(params.imageId),
    level: String(params.level),
    row: String(params.row),
    col: String(params.col)
  }).toString()
  return `${BASE_URL}/tile?${queryString}`
}

/**
 * 获取金字塔层级信息
 */
export function getLevelInfo(id: number) {
  return request.get<string>(`${BASE_URL}/${id}/levels`)
}

/**
 * 批次管理API接口
 */
import request, { PageData } from '@/utils/request'
import { createApiPath, SERVICES } from '@/config/services'
import type { BatchVO, BatchDTO, BatchQueryDTO } from '@/types/batch'

const BASE_URL = createApiPath(SERVICES.BIZ, '/api/v1/batches')

// 调试日志：确认 BASE_URL 正确
if (import.meta.env.DEV) {
  console.log('[Batches API] BASE_URL:', BASE_URL)
}

/**
 * 分页查询批次列表
 */
export function getBatchPage(query: BatchQueryDTO) {
  return request.post<PageData<BatchVO>>(`${BASE_URL}/page`, query)
}

/**
 * 获取项目下的所有批次
 */
export function getBatchesByProject(projectId: number) {
  return request.get<BatchVO[]>(`${BASE_URL}/by-project/${projectId}`)
}

/**
 * 获取批次详情
 */
export function getBatchById(id: number) {
  return request.get<BatchVO>(`${BASE_URL}/${id}`)
}

/**
 * 创建批次
 */
export function createBatch(data: BatchDTO) {
  return request.post<BatchVO>(BASE_URL, data)
}

/**
 * 更新批次
 */
export function updateBatch(id: number, data: BatchDTO) {
  return request.put(`${BASE_URL}/${id}`, data)
}

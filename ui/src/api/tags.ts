/**
 * 标签管理API接口
 */
import request from '@/utils/request'
import { createApiPath, SERVICES } from '@/config/services'
import type { Tag, BatchAssignTagsDTO } from '@/types/tag'

const BASE_URL = createApiPath(SERVICES.BIZ, '/api/v1/tags')

// 调试日志：确认 BASE_URL 正确
if (import.meta.env.DEV) {
  console.log('[Tags API] BASE_URL:', BASE_URL)
}

/**
 * 获取标签树形结构
 */
export function getTagTree(category?: string) {
  return request.get<string>(`${BASE_URL}/tree`, {
    params: category ? { category } : {}
  })
}

/**
 * 获取所有标签列表
 */
export function getAllTags() {
  return request.get<Tag[]>(BASE_URL)
}

/**
 * 根据项目ID获取标签列表
 */
export function getTagsByProject(projectId: number) {
  return request.get<Tag[]>(`${BASE_URL}/project/${projectId}`)
}

/**
 * 创建标签
 */
export function createTag(data: Tag) {
  return request.post<Tag>(BASE_URL, data)
}

/**
 * 批量给资产打标
 */
export function batchAssignTags(data: BatchAssignTagsDTO) {
  return request.post(`${BASE_URL}/batch-assign`, data)
}

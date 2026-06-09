/**
 * 项目管理API接口
 */
import request, { PageData } from '@/utils/request'
import { createApiPath, SERVICES } from '@/config/services'
import type { ProjectVO, ProjectDTO, ProjectQueryDTO } from '@/types/project'

const BASE_URL = createApiPath(SERVICES.BIZ, '/api/v1/projects')

// 调试日志：确认 BASE_URL 正确
if (import.meta.env.DEV) {
  console.log('[Projects API] BASE_URL:', BASE_URL)
}

/**
 * 分页查询项目列表
 */
export function getProjectPage(query: ProjectQueryDTO) {
  return request.post<PageData<ProjectVO>>(`${BASE_URL}/page`, query)
}

/**
 * 获取所有项目列表（不分页，用于下拉选择）
 */
export function getAllProjects() {
  return request.get<ProjectVO[]>(`${BASE_URL}/list`)
}

/**
 * 获取项目详情
 */
export function getProjectById(id: number) {
  return request.get<ProjectVO>(`${BASE_URL}/${id}`)
}

/**
 * 创建项目
 */
export function createProject(data: ProjectDTO) {
  return request.post<ProjectVO>(BASE_URL, data)
}

/**
 * 更新项目
 */
export function updateProject(id: number, data: ProjectDTO) {
  return request.put(`${BASE_URL}/${id}`, data)
}

/**
 * 归档项目（删除）
 */
export function archiveProject(id: number) {
  return request.delete(`${BASE_URL}/${id}`)
}

/**
 * 获取项目统计信息
 */
export function getProjectStats(id: number) {
  return request.get<string>(`${BASE_URL}/${id}/stats`)
}

/**
 * 项目相关类型定义
 */

// 项目实体
export interface Project {
  projectId: number
  name: string
  code: string
  managerId?: number
  ethicsCode?: string
  privacyLevel?: number  // 1:公开, 2:脱敏, 3:绝密
  description?: string
  targetClasses?: string // JSON字符串
  status?: string  // active/archived/deleted
  createBy?: number
  createTime: string
  updateBy?: number
  updateTime: string
  delFlag?: boolean
}

// 项目VO（视图对象）
export interface ProjectVO extends Project {
  // 可以扩展额外的展示字段
  stats?: {
    batchCount: number
    imageCount: number
    annotatedCount: number
  }
}

// 项目DTO（创建/更新）
export interface ProjectDTO {
  name: string
  code: string
  managerId?: number
  ethicsCode?: string
  privacyLevel?: number  // 1:公开, 2:脱敏, 3:绝密
  description?: string
  targetClasses?: string
  status?: string  // active/archived/deleted
}

// 项目查询DTO
export interface ProjectQueryDTO {
  current: number
  size: number
  name?: string
  status?: string  // active/archived/deleted
  managerId?: number
  startDate?: string
  endDate?: string
}

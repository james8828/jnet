/**
 * 标签相关类型定义
 */

// 标签分类枚举
export enum TagCategory {
  ORGAN_TYPE = 'ORGAN_TYPE', // 组织类型
  DISEASE_TYPE = 'DISEASE_TYPE', // 疾病类型
  QUALITY = 'QUALITY', // 质量标签
  CUSTOM = 'CUSTOM' // 自定义
}

// 标签实体
export interface Tag {
  tagId: number
  name: string
  code: string
  category?: TagCategory
  parentId?: number
  color?: string
  description?: string
  sortOrder?: number
  createBy?: number
  createTime: string
  updateBy?: number
  updateTime: string
  delFlag?: boolean
  children?: Tag[] // 树形结构子节点
}

// 批量打标DTO
export interface BatchAssignTagsDTO {
  assetIds: number[] // 资产ID列表（图像ID）
  tagIds: number[] // 标签ID列表
}

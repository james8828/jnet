import request from '@/utils/request'
import { createApiPath, SERVICES } from '@/config/services'
import type { AnnotationDTO, AnnotationFeature } from '@/types/annotation'

// 调试日志：确认服务配置正确
if (import.meta.env.DEV) {
  console.log('[Annotations API] SERVICES:', SERVICES)
  console.log('[Annotations API] ANNO prefix:', SERVICES.ANNO)
}

/**
 * 标注管理 API
 */

/**
 * 添加标注
 */
export function addAnnotation(data: AnnotationDTO) {
  return request.post<string>(createApiPath(SERVICES.ANNO, '/api/v1/annotation'), data)
}

/**
 * 删除标注
 */
export function deleteAnnotation(id: number) {
  return request.delete<void>(createApiPath(SERVICES.ANNO, `/api/v1/annotation/${id}`))
}

/**
 * 更新标注
 */
export function updateAnnotation(data: AnnotationDTO) {
  return request.put<void>(createApiPath(SERVICES.ANNO, '/api/v1/annotation'), data)
}

/**
 * 查询切片的所有标注
 */
export function getAnnotations(slideId: number) {
  return request.post<AnnotationFeature[]>(createApiPath(SERVICES.ANNO, '/api/v1/annotation/selectLists'), { slideId })
}

/**
 * 填充标注（移除孔洞）
 */
export function paddingAnnotation(annotationId: number) {
  return request.put<void>(createApiPath(SERVICES.ANNO, `/api/v1/annotation/padding/${annotationId}`))
}

/**
 * 复制/粘贴标注
 * @param annotationId 原始标注ID
 * @param tagId 新标注的标签ID（可选，不传则使用原标签）
 */
export function stickupAnnotation(annotationId: number, tagId?: number) {
  return request.post<void>(createApiPath(SERVICES.ANNO, '/api/v1/annotation/stickup'), { annotationId, tagId })
}

/**
 * 合并预览
 */
export function mergePreview(annotationIds: number[]) {
  return request.post<any>(createApiPath(SERVICES.ANNO, '/api/v1/annotation/mergePreview'), { markingIdList: annotationIds })
}

/**
 * 标注布尔运算（并集/差集）
 */
export function annotationOperation(data: {
  annotationId: number
  geometry: any
  operation: 'union' | 'difference'
  check?: boolean
}) {
  return request.post<any>(createApiPath(SERVICES.ANNO, '/api/v1/annotation/updateOperation'), data)
}

/**
 * 批量操作标注
 */
export function batchAnnotation(data: {
  slideId: number
  list: Array<{
    annotationId: number
    operation: 'update' | 'delete'
    geom?: any
  }>
}) {
  return request.post<any[]>(createApiPath(SERVICES.ANNO, '/api/v1/annotation/batch'), data)
}

/**
 * 计算两个标注之间的距离
 */
export function getDistance(data: {
  annotationIdOne: number
  annotationTypeOne: string
  annotationIdTwo: number
  annotationTypeTwo: string
}) {
  return request.post<any>(createApiPath(SERVICES.ANNO, '/api/v1/annotation/getDistance'), data)
}

/**
 * 撤销操作
 */
export function undoAnnotation(slideId: number) {
  return request.post<void>(createApiPath(SERVICES.ANNO, `/api/v1/annotation/undoAnnotation/${slideId}`))
}

/**
 * 重做操作
 */
export function redoAnnotation(slideId: number) {
  return request.post<void>(createApiPath(SERVICES.ANNO, `/api/v1/annotation/redoAnnotation/${slideId}`))
}

/**
 * 清除撤销/重做栈
 */
export function clearUndoRedoStack(slideId: number) {
  return request.post<void>(createApiPath(SERVICES.ANNO, `/api/v1/annotation/clear/${slideId}`))
}

/**
 * 检查撤销/重做状态
 */
export function checkUndoRedoStatus(slideId: number) {
  return request.post<{ undo: boolean; redo: boolean }>(
    createApiPath(SERVICES.ANNO, `/api/v1/annotation/checkUndoAndRedoStatus/${slideId}`)
  )
}

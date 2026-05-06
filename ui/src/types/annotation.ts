/**
 * 标注相关类型定义
 */

/**
 * 标注数据传输对象
 */
export interface AnnotationDTO {
  annotationId?: number
  imageId?: number
  slideId: number
  area?: number
  perimeter?: number
  description?: string
  tagId?: number
  geom: any // GeoJSON Geometry
  geomType?: string
  creationSource?: 'AI_PRE_ANNOTATION' | 'MANUAL_DRAWING' | 'AUTO_SEGMENTATION'
  createBy?: number
  createTime?: string
  updateBy?: number
  updateTime?: string
}

/**
 * 标注要素（GeoJSON Feature）
 */
export interface AnnotationFeature {
  type: 'Feature'
  id?: number
  geometry: any
  properties: {
    annotationId?: number
    slideId?: number
    imageId?: number
    tagId?: number
    geomType?: string
    area?: number
    perimeter?: number
    description?: string
    creationSource?: string
    createBy?: number
    createTime?: string
    updateBy?: number
    updateTime?: string
  }
}

/**
 * 标注工具模式
 */
export type AnnotationTool = 
  | 'select'        // 选择模式
  | 'draw-point'    // 绘制点
  | 'draw-line'     // 绘制线
  | 'draw-polygon'  // 绘制多边形
  | 'edit'          // 编辑模式
  | 'measure'       // 测量模式

/**
 * 标注操作类型
 */
export type AnnotationAction = 
  | 'add'           // 新增
  | 'update'        // 更新
  | 'delete'        // 删除
  | 'union'         // 并集
  | 'difference'    // 差集
  | 'padding'       // 填充
  | 'stickup'       // 复制粘贴

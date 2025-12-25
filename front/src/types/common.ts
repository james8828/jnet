/**
 * 通用接口返回类型
 */
export interface R<T> {
  success: boolean
  code: number
  msg: string
  data?: T
}

/**
 * 分页请求基类
 */
export interface PageQuery {
  currentPage: number
  pageSize: number
}

/**
 * 分页查询返回对象
 */
export interface Page<T> {
  size: number
  current: number
  total: number
  records?: T[]
}

/**
 * 通用分片上传
 */
export interface ChunkUploadVO {
  name: string
  uploadId: string
  chunkIndex: number
  chunkTotal: number
  chunkSize: number
  chunkMd5: string
  chunk: File
  chunks: ChunkUploadVO[]
}

/**
 * 通用附件类型
 */
export interface Attachment {
  attachmentId: number
  attachmentName: string
  attachmentPath: string
  attachmentCode: string
  attachmentExt: string
  attachmentSize: number
  createBy: number
  createTime: string
}

export interface AttachmentVO {
  attachmentCode?: string
  attachmentName?: string
  pageNum?: number
  pageSize?: number
}

export interface Annotation {
  /**
   * 主键id
   */
  annotationId: number | null;

  /**
   * 面积
   */
  area: string | null; // BigDecimal -> 字符串避免精度丢失

  /**
   * 周长
   */
  perimeter: string | null; // BigDecimal -> 字符串避免精度丢失

  /**
   * 轮廓描述
   */
  description: string | null;

  /**
   * 标签id
   */
  categoryId: number | null;

  /**
   * 轮廓坐标 Geometry（在前端通常用 GeoJSON 表示）
   */
  geometry: any; // 可以是 GeoJSON 对象或 WKT 字符串

  /**
   * 轮廓类型
   */
  locationType: string | null;

  /**
   * 标注类型(AI表示AI算出的标注，Draw表示前端绘制的标注)
   */
  annotationType: string | null;

  /**
   * 标注创建者
   */
  createBy: number | null;

  /**
   * 创建时间
   */
  createTime: string | null; // ISO8601 格式字符串

  /**
   * 更新者
   */
  updateBy: number | null;

  /**
   * 更新时间
   */
  updateTime: string | null; // ISO8601 格式字符串

  /**
   * 切片id
   */
  slideId: number | null;

  /**
   * geojson中数据id
   */
  jsonId: string | null;
}






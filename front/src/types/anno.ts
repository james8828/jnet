
// AnnotationUpdateVo 类型定义示例
export interface AnnotationUpdateVo {
    id: number;
    geometry?: string;
    categoryId?: number;
}

// Geometry 类型定义（简化）
export interface Geometry {
    type: string;
    coordinates: any[];
}

// AnnotationFeature 类型定义（对应后端返回的 Feature 结构）
export interface AnnotationFeature {
    type: string;
    geometry: Geometry;
    properties: Record<string, any>;
}

// AnnotationQuery 查询参数
export interface AnnotationQuery {
    slideId: number;
}




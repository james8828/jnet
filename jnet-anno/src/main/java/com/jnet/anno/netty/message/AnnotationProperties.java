package com.jnet.anno.netty.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标注属性类 - 符合标准 GeoJSON Feature 的 properties 结构
 * 
 * @author mugw
 * @version 3.0
 * @since 2025/5/22 13:55:08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationProperties {

    /**
     * 标注ID（主键）
     */
    private Long annotationId;

    /**
     * 切片ID
     */
    private Long slideId;

    /**
     * 图像ID
     */
    private Long imageId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 几何类型：Point / LineString / Polygon / MultiPolygon
     */
    private String geomType;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 标注面积（计算字段）
     */
    private Double area;

    /**
     * 标注周长（计算字段）
     */
    private Double perimeter;
}
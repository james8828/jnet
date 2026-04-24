package com.jnet.anno.vo.anno;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 标注数据传输对象（DTO）
 * <p>
 * 用于前端与后端之间传输标注数据，支持新增、更新等操作。
 * 包含标注的几何信息、属性信息和元数据。
 * </p>
 *
 * @author JNet Team
 * @since 2025-05-22
 */
@Schema(description = "标注数据传输对象")
@Data
public class AnnotationDTO {

    /**
     * 标注ID
     * <p>
     * 标注记录的唯一标识符。
     * 新增操作时可为 null，由后端自动生成；
     * 更新操作时必须提供，用于定位要更新的标注记录。
     * </p>
     */
    @Schema(description = "标注ID（新增时可选，更新时必填）", example = "12345")
    private Long annotationId;

    /**
     * 图像ID
     * <p>
     * 关联的原始图像标识符，用于追溯标注来源。
     * </p>
     */
    @Schema(description = "图像ID", example = "1001")
    private Long imageId;

    /**
     * 切片ID
     * <p>
     * 病理切片的唯一标识符，必填字段。
     * 每个标注必须归属于一个特定的切片。
     * </p>
     */
    @Schema(description = "切片ID（必填）", example = "2001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long slideId;

    /**
     * 标注区域面积
     * <p>
     * 标注轮廓所围成的区域面积，单位为平方像素。
     * 对于点（Point）和线（LineString）类型，此值为 0 或 null。
     * </p>
     */
    @Schema(description = "标注面积（平方像素）", example = "1234.56")
    private BigDecimal area;

    /**
     * 标注轮廓周长
     * <p>
     * 标注轮廓的周长长度，单位为像素。
     * 对于点（Point）类型，此值为 0 或 null。
     * </p>
     */
    @Schema(description = "标注周长（像素）", example = "150.25")
    private BigDecimal perimeter;

    /**
     * 标注描述信息
     * <p>
     * 对标注内容的文字说明，可选字段。
     * 可用于记录病理特征、诊断意见等备注信息。
     * </p>
     */
    @Schema(description = "标注描述", example = "肿瘤区域，边界清晰")
    private String description;

    /**
     * 标签ID
     * <p>
     * 关联的分类标签标识符，用于标注的类型分类。
     * 例如：肿瘤、炎症、坏死等病理分类。
     * </p>
     */
    @Schema(description = "标签ID", example = "10")
    private Long tagId;

    /**
     * 几何图形数据
     * <p>
     * 标注的空间几何信息，使用 JTS Geometry 对象表示。
     * 支持以下类型：
     * <ul>
     *   <li>Polygon - 多边形（区域标注）</li>
     *   <li>LineString - 线段（线性标注）</li>
     *   <li>Point - 点（点位标注）</li>
     *   <li>MultiPolygon - 多多边形</li>
     * </ul>
     * 前端通过 GeoJSON 格式传递，后端自动转换为 JTS Geometry。
     * </p>
     */
    @Schema(description = "几何图形（GeoJSON格式，必填）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "{\"type\":\"Polygon\",\"coordinates\":[[[116.3,39.9],[116.4,39.9],[116.4,40.0],[116.3,40.0],[116.3,39.9]]]}")
    private Geometry geom;

    /**
     * 几何类型
     * <p>
     * 几何图形的类型标识，与 geometry 字段保持一致。
     * 可选值：
     * <ul>
     *   <li>POLYGON - 多边形</li>
     *   <li>LINESTRING - 线段</li>
     *   <li>POINT - 点</li>
     *   <li>MULTIPOLYGON - 多多边形</li>
     *   <li>MULTILINESTRING - 多线段</li>
     *   <li>MULTIPOINT - 多点</li>
     * </ul>
     * </p>
     */
    @Schema(description = "几何类型", example = "POLYGON",
            allowableValues = {"POLYGON", "LINESTRING", "POINT", "MULTIPOLYGON", "MULTILINESTRING", "MULTIPOINT"})
    private String geomType;

    /**
     * 标注来源
     * <p>
     * 标注的创建来源，可选值：
     * <ul>
     *   <li>AI_PRE_ANNOTATION - AI 预标注</li>
     *   <li>MANUAL_DRAWING - 手动绘制</li>
     *   <li>AUTO_SEGMENTATION - 自动分割</li>
     * </ul>
     * </p>
     */
    @Schema(description = "标注来源", example = "MANUAL_DRAWING",
            allowableValues = {"AI_PRE_ANNOTATION", "MANUAL_DRAWING", "AUTO_SEGMENTATION"})
    private String creationSource;

    /**
     * 标注创建者ID
     * <p>
     * 创建此标注的用户标识符。
     * 通常由后端从安全上下文自动填充，前端无需传递。
     * </p>
     */
    @Schema(description = "创建者ID（后端自动填充）", example = "1001", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createBy;

    /**
     * 创建时间
     * <p>
     * 标注记录的创建时间戳。
     * 由后端自动生成，格式为 ISO 8601（yyyy-MM-dd HH:mm:ss）。
     * </p>
     */
    @Schema(description = "创建时间（后端自动生成）", example = "2025-04-16 10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private Date createTime;

    /**
     * 最后更新者ID
     * <p>
     * 最后修改此标注的用户标识符。
     * 每次更新操作时由后端自动更新。
     * </p>
     */
    @Schema(description = "更新者ID（后端自动维护）", example = "1001", accessMode = Schema.AccessMode.READ_ONLY)
    private Long updateBy;

    /**
     * 最后更新时间
     * <p>
     * 标注记录的最后更新时间戳。
     * 每次更新操作时由后端自动更新，格式为 ISO 8601。
     * </p>
     */
    @Schema(description = "更新时间（后端自动维护）", example = "2025-04-16 14:20:00", accessMode = Schema.AccessMode.READ_ONLY)
    private Date updateTime;
}

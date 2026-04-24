package com.jnet.anno.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 矢量标注实体
 * <p>
 * 基于 Hibernate Spatial 的标注实体，支持空间查询和空间索引。
 * 对应数据库表：biz_annotation
 * </p>
 *
 * @author JNet Team
 * @version 2.0
 * @since 2026-04-24
 */
@Data
@Entity
@Table(name = "biz_annotation", indexes = {
        @Index(name = "idx_vec_spatial", columnList = "geom"),
        @Index(name = "idx_vec_slide", columnList = "slide_id, is_active"),
        @Index(name = "idx_vec_image", columnList = "image_id, is_active"),
        @Index(name = "idx_vec_project", columnList = "project_id, is_active"),
        @Index(name = "idx_vec_batch", columnList = "batch_id, is_active"),
        @Index(name = "idx_vec_tag", columnList = "tag_id, is_active"),
        @Index(name = "idx_vec_parent", columnList = "parent_annotation_id"),
        @Index(name = "idx_vec_review", columnList = "review_status, slide_id"),
        @Index(name = "idx_vec_slide_tag", columnList = "slide_id, tag_id, is_active"),
        @Index(name = "idx_vec_image_tag", columnList = "image_id, tag_id, is_active"),
        @Index(name = "idx_vec_create_time", columnList = "create_time"),
        @Index(name = "idx_vec_update_time", columnList = "update_time"),
        @Index(name = "idx_vec_lod", columnList = "slide_id, lod_level, is_active"),
        @Index(name = "idx_vec_bbox", columnList = "bbox")
})
public class Annotation implements Serializable {

    /**
     * 主键ID（标注对象ID）
     */
    @Id
    @Column(name = "annotation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long annotationId;

    /**
     * 关联切片ID（主要查询维度）
     */
    @NotNull
    @Column(name = "slide_id")
    private Long slideId;

    /**
     * 关联图像ID（冗余字段）
     */
    @NotNull
    @Column(name = "image_id")
    private Long imageId;

    /**
     * 所属项目ID（冗余字段，便于按项目统计）
     */
    @Column(name = "project_id")
    private Long projectId;

    /**
     * 所属批次ID（冗余字段，便于按批次统计）
     */
    @Column(name = "batch_id")
    private Long batchId;

    /**
     * 关联标签ID
     */
    @NotNull
    @Column(name = "tag_id")
    private Long tagId;

    /**
     * 父标注ID（支持层级标注，如：脏器->组织）
     */
    @Column(name = "parent_annotation_id")
    private Long parentAnnotationId;

    /**
     * 标注类型 (POINT/LINESTRING/POLYGON/MULTIPOLYGON)
     */
    @NotNull
    @Column(name = "geom_type", length = 20)
    private String geomType;

    /**
     * PostGIS几何字段（主存储，支持空间索引和精确查询）
     */
    @Column(columnDefinition = "geometry")
    private Geometry geom;

    /**
     * GeoJSON格式坐标（可选备份，用于前端快速渲染）
     */
    @Column(name = "coordinates_geojson", columnDefinition = "jsonb")
    private String coordinatesGeojson;

    /**
     * LOD层级 (0=原始精度, 1-5=简化层级，数字越大越简化)
     */
    @Column(name = "lod_level")
    private Integer lodLevel = 0;

    /**
     * 简化后的几何（用于低分辨率快速渲染）
     */
    @Column(name = "simplified_geom", columnDefinition = "geometry")
    private Geometry simplifiedGeom;

    /**
     * 边界框（用于快速筛选、视口裁剪和碰撞检测）
     */
    @Column(name = "bbox", columnDefinition = "geometry")
    private Geometry bbox;

    /**
     * 置信度 (0-1)
     */
    @Column(name = "confidence")
    private Double confidence;

    /**
     * 标注区域面积（微米²）
     */
    @Column(name = "area")
    private BigDecimal area;

    /**
     * 周长（微米）
     */
    @Column(name = "perimeter")
    private BigDecimal perimeter;

    /**
     * 质心X坐标
     */
    @Column(name = "centroid_x")
    private Double centroidX;

    /**
     * 质心Y坐标
     */
    @Column(name = "centroid_y")
    private Double centroidY;

    /**
     * 标注描述信息
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 来源 (AI_PRE_ANNOTATION/MANUAL_DRAWING/AUTO_SEGMENTATION)
     */
    @Column(name = "creation_source", length = 20)
    private String creationSource;

    /**
     * 审核状态 (PENDING/APPROVED/REJECTED/MODIFIED)
     */
    @Column(name = "review_status", length = 20)
    private String reviewStatus = "PENDING";

    /**
     * 审核人ID
     */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    /**
     * 审核时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "review_time")
    private Date reviewTime;

    /**
     * 版本号（支持修改历史）
     */
    @Column(name = "version")
    private Integer version = 1;

    /**
     * 是否有效
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * 排序序号（同一层级内的显示顺序）
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 审计字段 - 创建者ID
     */
    @Column(name = "create_by")
    private Long createBy;

    /**
     * 审计字段 - 创建时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 审计字段 - 更新者ID
     */
    @Column(name = "update_by")
    private Long updateBy;

    /**
     * 审计字段 - 更新时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time")
    private Date updateTime;
}

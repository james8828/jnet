package com.jnet.anno.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 基于 Hibernate Spatial 的标注实体
 * 支持空间查询和空间索引
 * @author mu
 * @version 1.0
 * @since 2026/3/10
 */
@Data
@Entity
@Table(name = "t_annotation", indexes = {
        @Index(name = "idx_annotation_geometry", columnList = "contour"),
        @Index(name = "idx_annotation_slide_id", columnList = "slide_id")
})
public class SpatialAnnotation implements Serializable {

    @Id
    @Column(name = "annotation_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annotation_seq_generator")
    @SequenceGenerator(
        name = "annotation_seq_generator",
        sequenceName = "t_annotation_seq",
        allocationSize = 1
    )
  private Long annotationId;

    @Column(precision= 20, scale = 10)
    private BigDecimal area;

    @Column(precision = 20, scale = 10)
    private BigDecimal perimeter;

    @Column(length = 500)
    private String description;

    @Column(name = "tag_id")
    private Long tagId;

    /**
     * 使用 Hibernate Spatial 的 Geometry 类型
     * 自动映射到 PostGIS 的 geometry 列
     */
    @Column(columnDefinition = "geometry")
//    @JdbcTypeCode(org.hibernate.type.SqlTypes.GEOMETRY)
    private Geometry contour;

    @Column(name = "location_type")
    private String locationType;

    @Column(name = "annotation_type")
    private String annotationType;

    @Column(name = "create_by")
    private Long createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time")
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time")
    private Date updateTime;

    @Column(name = "update_by")
    private Long updateBy;

    @NotNull
    @Column(name = "slide_id")
    private Long slideId;

    @Column(name = "json_id")
    private String jsonId;
}


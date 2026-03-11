package com.jnet.anno.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;
import java.util.Date;

/**
 * 基于 Hibernate Spatial 的测量标注实体
 * @author mugw
 * @version 1.0
 * @since 2025/3/10
 */
@Data
@Entity
@Table(name = "t_measure")
public class SpatialMeasure implements Serializable {

    /**
     * 主键 id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "measure_id")
    private Long measureId;

    /**
     * 切片 id
     */
    @Column(name = "slide_id")
    private Long slideId;

    /**
     * 标注类型 (AI 表示 AI 算出的标注，Draw 表示前端绘制的标注，Measure 表示测量工具数据)
     */
    @Column(name = "annotation_type")
    private String annotationType;

    /**
     * 面积
     */
    @Column(name = "area")
    private String area;

    /**
     * 周长
     */
    @Column(name = "perimeter")
    private String perimeter;

    /**
     * 标注名称
     */
    @Column(name = "number")
    private Long number;

    /**
     * 测量轮廓类型 (0:正常，表示有关系，默认为 0)
     */
    @Column(name = "measure_type")
    private Integer measureType;

    /**
     * 测量关系
     */
    @Column(name = "measure_relation")
    private String measureRelation;

    /**
     * 测量轮廓表示名称:L
     */
    @Column(name = "measure_name")
    private String measureName;

    /**
     * 测量轮廓标识：1
     */
    @Column(name = "measure_number")
    private Integer measureNumber;

    /**
     * 平均间距
     */
    @Column(name = "mean_distance")
    private Double meanDistance;

    /**
     * 最大间距
     */
    @Column(name = "max_distance")
    private Double maxDistance;

    /**
     * 最小间距
     */
    @Column(name = "min_distance")
    private Double minDistance;

    /**
     * 内角
     */
    @Column(name = "inner_angle")
    private String innerAngle;

    /**
     * 外角
     */
    @Column(name = "exterior_angle")
    private String exteriorAngle;

    /**
     * 中心
     */
    @Column(name = "center_point")
    private String centerPoint;

    /**
     * 标注数据类型 (LineString,Polygon,point,pc,p,L)
     */
    @Column(name = "location_type")
    private String locationType;

    /**
     * 周长（圆）
     */
    @Column(name = "radius")
    private String radius;

    /**
     * 标注数据（JSON 格式）- 使用空间类型
     */
    @Column(name = "contour", columnDefinition = "geometry")
    private Geometry geometry;

    /**
     * 创建者
     */
    @Column(name = "create_by")
    private Long createBy;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新者
     */
    @Column(name = "update_by")
    private Long updateBy;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 标注名称
     */
    @Column(name = "measure_full_name")
    private String measureFullName;

    private static final long serialVersionUID = 1L;


    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        SpatialMeasure other = (SpatialMeasure) that;
        return (this.getMeasureId() == null ? other.getMeasureId() == null : this.getMeasureId().equals(other.getMeasureId()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getMeasureId() == null) ? 0 : getMeasureId().hashCode());
        return result;
    }
}

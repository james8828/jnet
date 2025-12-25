package com.jnet.anno.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author mugw
 * @version 1.0
 * @description 标注表
 * @date 2025/5/21 14:33:43
 */
@Schema(description = "标注表")
@TableName(value ="t_annotation")
@Data
public class Annotation implements Serializable {
    /**
     * 主键id
     */
    @TableId("annotation_id")
    private Long annotationId;

    /**
     * 面积
     */
    @Schema(description = "面积")
    private BigDecimal area;

    /**
     * 周长
     */
    private BigDecimal perimeter;

    /**
     * 轮廓描述
     */
    private String description;

    /**
     * 标签id
     */
    @TableField("category_id")
    private Long tagId;

    /**
     * 轮廓坐标625
     */
    @NotNull(message = "{ARGUMENT_INVALID}")
    @TableField("contour40000")
    private Geometry geometry;

    /**
     * 轮廓类型
     */
    private String locationType;

    /**
     * 标注类型(AI表示AI算出的标注，Draw表示前端绘制的标注，Measure表示测量数据)
     */
    private String annotationType;

    /**
     * 标注创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 切片id
     */
    @NotNull(message = "{MarkingDelIn.slideId.notNull}")
    private Long slideId;

    /**
     * geojson中数据id
     */
    @TableField("id")
    private String jsonId;

    private Long singleSlideId;

    @TableField(exist = false)
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
        Annotation other = (Annotation) that;
        return (this.getAnnotationId() == null ? other.getAnnotationId() == null : this.getAnnotationId().equals(other.getAnnotationId()))
            && (this.getArea() == null ? other.getArea() == null : this.getArea().equals(other.getArea()))
            && (this.getPerimeter() == null ? other.getPerimeter() == null : this.getPerimeter().equals(other.getPerimeter()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getTagId() == null ? other.getTagId() == null : this.getTagId().equals(other.getTagId()))
            && (this.getGeometry() == null ? other.getGeometry() == null : this.getGeometry().equals(other.getGeometry()))
            && (this.getLocationType() == null ? other.getLocationType() == null : this.getLocationType().equals(other.getLocationType()))
            && (this.getAnnotationType() == null ? other.getAnnotationType() == null : this.getAnnotationType().equals(other.getAnnotationType()))
            && (this.getCreateBy() == null ? other.getCreateBy() == null : this.getCreateBy().equals(other.getCreateBy()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateBy() == null ? other.getUpdateBy() == null : this.getUpdateBy().equals(other.getUpdateBy()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getSlideId() == null ? other.getSlideId() == null : this.getSlideId().equals(other.getSlideId()))
            && (this.getJsonId() == null ? other.getJsonId() == null : this.getJsonId().equals(other.getJsonId()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getAnnotationId() == null) ? 0 : getAnnotationId().hashCode());
        result = prime * result + ((getArea() == null) ? 0 : getArea().hashCode());
        result = prime * result + ((getPerimeter() == null) ? 0 : getPerimeter().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getTagId() == null) ? 0 : getTagId().hashCode());
        result = prime * result + ((getGeometry() == null) ? 0 : getGeometry().hashCode());
        result = prime * result + ((getLocationType() == null) ? 0 : getLocationType().hashCode());
        result = prime * result + ((getAnnotationType() == null) ? 0 : getAnnotationType().hashCode());
        result = prime * result + ((getCreateBy() == null) ? 0 : getCreateBy().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateBy() == null) ? 0 : getUpdateBy().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getSlideId() == null) ? 0 : getSlideId().hashCode());
        result = prime * result + ((getJsonId() == null) ? 0 : getJsonId().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", contourId=").append(annotationId);
        sb.append(", area=").append(area);
        sb.append(", perimeter=").append(perimeter);
        sb.append(", description=").append(description);
        sb.append(", tagId=").append(tagId);
        sb.append(", contour=").append(geometry);
        sb.append(", locationType=").append(locationType);
        sb.append(", annotationType=").append(annotationType);
        sb.append(", createBy=").append(createBy);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateBy=").append(updateBy);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", slideId=").append(slideId);
        sb.append(", jsonId=").append(jsonId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
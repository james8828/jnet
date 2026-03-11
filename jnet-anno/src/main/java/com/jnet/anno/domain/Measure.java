package com.jnet.anno.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;
import java.util.Date;


/**
 * @author mugw
 * @version 1.0
 * @description 测量标注表
 * @date 2025/5/21 14:33:43
 */
@TableName(value ="t_measure")
@Data
public class Measure implements Serializable {
    /**
     * 主键id
     */
    @TableId
    private Long measureId;

    /**
     * 切片id
     */
    private Long slideId;

    /**
     * 标注类型(AI表示AI算出的标注，Draw表示前端绘制的标注，Measure表示测量工具数据)
     */
    private String annotationType;

    /**
     * 面积
     */
    private String area;

    /**
     * 周长
     */
    private String perimeter;

    /**
     * 标注名称
     */
    private Long number;

    /**
     * 测量轮廓类型(0:正常,表示有关系,默认为0)
     */
    private Integer measureType;

    /**
     * 测量关系
     */
    private String measureRelation;

    /**
     * 测量轮廓表示名称:L
     */
    private String measureName;

    /**
     * 测量轮廓标识：1
     */
    private Integer measureNumber;

    /**
     * 平均间距
     */
    private Double meanDistance;

    /**
     * 最大间距
     */
    private Double maxDistance;

    /**
     * 最小间距
     */
    private Double minDistance;

    /**
     * 内角
     */
    private String innerAngle;

    /**
     * 外角
     */
    private String exteriorAngle;

    /**
     * 中心
     */
    private String centerPoint;

    /**
     * 标注数据类型(LineString,Polygon,point,pc,p,L)
     */
    private String locationType;

    /**
     * 周长（圆）
     */
    private String radius;

    /**
     * 标注数据（JSON格式）
     */
    @TableField("contour")
    private Geometry geometry;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 标注名称
     */
    private String measureFullName;

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
        Measure other = (Measure) that;
        return (this.getMeasureId() == null ? other.getMeasureId() == null : this.getMeasureId().equals(other.getMeasureId()))
            && (this.getSlideId() == null ? other.getSlideId() == null : this.getSlideId().equals(other.getSlideId()))
            && (this.getAnnotationType() == null ? other.getAnnotationType() == null : this.getAnnotationType().equals(other.getAnnotationType()))
            && (this.getArea() == null ? other.getArea() == null : this.getArea().equals(other.getArea()))
            && (this.getPerimeter() == null ? other.getPerimeter() == null : this.getPerimeter().equals(other.getPerimeter()))
            && (this.getNumber() == null ? other.getNumber() == null : this.getNumber().equals(other.getNumber()))
            && (this.getMeasureType() == null ? other.getMeasureType() == null : this.getMeasureType().equals(other.getMeasureType()))
            && (this.getMeasureRelation() == null ? other.getMeasureRelation() == null : this.getMeasureRelation().equals(other.getMeasureRelation()))
            && (this.getMeasureName() == null ? other.getMeasureName() == null : this.getMeasureName().equals(other.getMeasureName()))
            && (this.getMeasureNumber() == null ? other.getMeasureNumber() == null : this.getMeasureNumber().equals(other.getMeasureNumber()))
            && (this.getMeanDistance() == null ? other.getMeanDistance() == null : this.getMeanDistance().equals(other.getMeanDistance()))
            && (this.getMaxDistance() == null ? other.getMaxDistance() == null : this.getMaxDistance().equals(other.getMaxDistance()))
            && (this.getMinDistance() == null ? other.getMinDistance() == null : this.getMinDistance().equals(other.getMinDistance()))
            && (this.getInnerAngle() == null ? other.getInnerAngle() == null : this.getInnerAngle().equals(other.getInnerAngle()))
            && (this.getExteriorAngle() == null ? other.getExteriorAngle() == null : this.getExteriorAngle().equals(other.getExteriorAngle()))
            && (this.getCenterPoint() == null ? other.getCenterPoint() == null : this.getCenterPoint().equals(other.getCenterPoint()))
            && (this.getLocationType() == null ? other.getLocationType() == null : this.getLocationType().equals(other.getLocationType()))
            && (this.getRadius() == null ? other.getRadius() == null : this.getRadius().equals(other.getRadius()))
            && (this.getCreateBy() == null ? other.getCreateBy() == null : this.getCreateBy().equals(other.getCreateBy()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateBy() == null ? other.getUpdateBy() == null : this.getUpdateBy().equals(other.getUpdateBy()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getMeasureFullName() == null ? other.getMeasureFullName() == null : this.getMeasureFullName().equals(other.getMeasureFullName()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getMeasureId() == null) ? 0 : getMeasureId().hashCode());
        result = prime * result + ((getSlideId() == null) ? 0 : getSlideId().hashCode());
        result = prime * result + ((getAnnotationType() == null) ? 0 : getAnnotationType().hashCode());
        result = prime * result + ((getArea() == null) ? 0 : getArea().hashCode());
        result = prime * result + ((getPerimeter() == null) ? 0 : getPerimeter().hashCode());
        result = prime * result + ((getNumber() == null) ? 0 : getNumber().hashCode());
        result = prime * result + ((getMeasureType() == null) ? 0 : getMeasureType().hashCode());
        result = prime * result + ((getMeasureRelation() == null) ? 0 : getMeasureRelation().hashCode());
        result = prime * result + ((getMeasureName() == null) ? 0 : getMeasureName().hashCode());
        result = prime * result + ((getMeasureNumber() == null) ? 0 : getMeasureNumber().hashCode());
        result = prime * result + ((getMeanDistance() == null) ? 0 : getMeanDistance().hashCode());
        result = prime * result + ((getMaxDistance() == null) ? 0 : getMaxDistance().hashCode());
        result = prime * result + ((getMinDistance() == null) ? 0 : getMinDistance().hashCode());
        result = prime * result + ((getInnerAngle() == null) ? 0 : getInnerAngle().hashCode());
        result = prime * result + ((getExteriorAngle() == null) ? 0 : getExteriorAngle().hashCode());
        result = prime * result + ((getCenterPoint() == null) ? 0 : getCenterPoint().hashCode());
        result = prime * result + ((getLocationType() == null) ? 0 : getLocationType().hashCode());
        result = prime * result + ((getRadius() == null) ? 0 : getRadius().hashCode());
        result = prime * result + ((getCreateBy() == null) ? 0 : getCreateBy().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateBy() == null) ? 0 : getUpdateBy().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getMeasureFullName() == null) ? 0 : getMeasureFullName().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", measureId=").append(measureId);
        sb.append(", slideId=").append(slideId);
        sb.append(", annotationType=").append(annotationType);
        sb.append(", area=").append(area);
        sb.append(", perimeter=").append(perimeter);
        sb.append(", number=").append(number);
        sb.append(", measureType=").append(measureType);
        sb.append(", measureRelation=").append(measureRelation);
        sb.append(", measureName=").append(measureName);
        sb.append(", measureNumber=").append(measureNumber);
        sb.append(", meanDistance=").append(meanDistance);
        sb.append(", maxDistance=").append(maxDistance);
        sb.append(", minDistance=").append(minDistance);
        sb.append(", innerAngle=").append(innerAngle);
        sb.append(", exteriorAngle=").append(exteriorAngle);
        sb.append(", centerPoint=").append(centerPoint);
        sb.append(", locationType=").append(locationType);
        sb.append(", radius=").append(radius);
        sb.append(", createBy=").append(createBy);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateBy=").append(updateBy);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", measureFullName=").append(measureFullName);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
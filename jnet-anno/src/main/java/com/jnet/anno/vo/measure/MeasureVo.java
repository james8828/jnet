package com.jnet.anno.vo.measure;

import com.jnet.anno.domain.Measure;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.locationtech.jts.geom.Geometry;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/30 09:22:39
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MeasureVo {

    @ExcelProperty(value = "测量人",  index = 9)
    private String createUserName;

    @JsonProperty(value = "point_count")
    @ExcelProperty(value = "总数",  index = 8)
    private long pointCount;

    @JsonProperty(value =  "measure_full_name")
    @ExcelProperty(value = "名称",  index = 0)
    private String measureFullName;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonProperty(value = "marking_id")
    @ExcelIgnore
    private Long measureId;

    /**
     * 切片id
     */
    @ExcelIgnore
    private Long slideId;

    /**
     * 标注类型(AI表示AI算出的标注，Draw表示前端绘制的标注，Measure表示测量工具数据)
     */
    @ExcelIgnore
    private String annotationType;

    /**
     * 面积
     */
    @ExcelProperty(value = "面积",  index = 2)
    private String area;

    /**
     * 周长
     */
    @ExcelProperty(value = "周长/长度",  index = 1)
    private String perimeter;

    /**
     * 标注名称
     */
    @ExcelIgnore
    private Long number;

    /**
     * 测量轮廓类型(0:正常,表示有关系,默认为0)
     */
    @ExcelIgnore
    private Integer measureType;

    /**
     * 测量关系
     */
    @ExcelIgnore
    private String measureRelation;

    /**
     * 测量轮廓表示名称:L
     */
    @ExcelIgnore
    private String measureName;

    /**
     * 测量轮廓标识：1
     */
    @ExcelIgnore
    private Integer measureNumber;

    /**
     * 平均间距
     */
    @JsonProperty(value = "mean_distance")
    @ExcelProperty(value = "平均间距",  index = 5)
    private Double meanDistance;

    /**
     * 最大间距
     */
    @JsonProperty(value =  "max_distance")
    @ExcelProperty(value = "最大间距",  index = 7)
    private Double maxDistance;

    /**
     * 最小间距
     */
    @JsonProperty(value =  "min_distance")
    @ExcelProperty(value = "最小间距",  index = 6)
    private Double minDistance;

    /**
     * 内角
     */
    @JsonProperty(value = "inner_angle")
    @ExcelProperty(value = "内角",  index = 3)
    private String innerAngle;

    /**
     * 外角
     */
    @JsonProperty(value = "exterior_angle")
    @ExcelProperty(value = "外角",  index = 4)
    private String exteriorAngle;

    /**
     * 中心
     */
    @ExcelIgnore
    private String centerPoint;

    /**
     * 标注数据类型(LineString,Polygon,point,pc,p,L)
     */
    @ExcelIgnore
    private String locationType;

    /**
     * 周长（圆）
     */
    @ExcelIgnore
    private String radius;

    /**
     * 标注数据（JSON格式）
     */
    @ExcelIgnore
    @TableField("contour")
    private Geometry geometry;

    /**
     * 创建者
     */
    @ExcelIgnore
    private Long createBy;

    /**
     * 创建时间
     */
    @JsonProperty(value = "create_time")
    @ExcelProperty(value = "创建时间",  index = 10)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新者
     */
    @ExcelIgnore
    private Long updateBy;

    /**
     * 更新时间
     */
    @ExcelIgnore
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    public static MeasureVo convert(Measure measure){
        MeasureVo measureVo = new MeasureVo();
        try {
            BeanUtils.copyProperties(measureVo,measure);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return measureVo;
    }
}

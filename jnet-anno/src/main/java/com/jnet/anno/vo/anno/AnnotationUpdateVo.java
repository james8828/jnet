package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author mugw
 * @version 1.0
 * @description 新增标注参数
 * @date 2025/5/22 09:40:28
 */
@Data
public class AnnotationUpdateVo {

    /**
     * 主键id
     */
    @NotNull(message = "{NO_ANNOTATION_DATA}")
    @JsonProperty("marking_id")
    private Long annotationId;

    /**
     * 面积
     */
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
    @JsonProperty("category_id")
    private Long tagId;

    /**
     * 轮廓
     */
    private Geometry geometry;

    /**
     * 轮廓类型
     */
    private String locationType;

    /**
     * 标注类型(AI表示AI算出的标注，Draw表示前端绘制的标注)
     */
    @JsonProperty("annotation_type")
    private String annotationType;

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
    @JsonProperty("slide_id")
    private Long slideId;

    /**
     * geojson中数据id
     */
    private String jsonId;
}

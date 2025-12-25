package com.jnet.anno.vo.anno;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "新增标注参数")
@Data
public class AnnotationAddReq {

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
    private Long tagId;

    /**
     * 轮廓坐标625
     */
    @NotNull(message = "{ARGUMENT_INVALID}")
    @TableField("contour")
    private Geometry geometry;

    /**
     * 轮廓类型
     */
    private String locationType;

    /**
     * 标注类型(AI表示AI算出的标注，Draw表示前端绘制的标注)
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
    private String jsonId;
}

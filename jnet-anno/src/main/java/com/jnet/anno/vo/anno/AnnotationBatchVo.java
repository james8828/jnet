package com.jnet.anno.vo.anno;

import com.jnet.anno.utils.MessageSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;
import java.math.BigDecimal;

/**
 * @author mugw
 * @version 1.0
 * @description 新增标注参数
 * @date 2025/5/22 09:40:28
 */
@Data
public class AnnotationBatchVo {

    /**
     * 主键id
     */
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
    @JsonProperty("location_type")
    private String locationType;

    @NotNull(message = "{NO_SLIDE_DATA}")
    @Schema(description = "操作：修改-UPDATE,删除-DELETE,添加-INSERT")
    private String operation;

    @Schema(description = "操作结果")
    private String message = MessageSource.M("OPERATE_SUCCEED");

    @Schema(description = "操作是否成功")
    private Boolean status = true;

    private String front_id;

}

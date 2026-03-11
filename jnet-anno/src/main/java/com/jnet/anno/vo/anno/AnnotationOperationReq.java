package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;


/**
 * @author .
 */
@Data
public class AnnotationOperationReq extends AnnotationReq{

    @NotNull(message = "{DescriptionUpdateVO.annotationId.isnull}")
    @Schema(description = "标注id")
    @JsonProperty("marking_id")
    private Long annotationId;

    @NotNull(message = "{ARGUMENT_INVALID}")
    @Schema(description = "(新图形) 标注坐标")
    private Geometry geometry;

    @NotBlank(message = "操作不可为空")
    @Schema(description = "要执行的操作(UNION:相交,DIFFERENCE:相差)")
    private String operation;

    @NotNull(message = "校验不可为空")
    @Schema(description = "校验")
    private Boolean check;

    @Schema(description = "分辨率")
    private String resolution;
}

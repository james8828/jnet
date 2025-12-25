package com.jnet.anno.vo.anno;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author mugw
 * @version 1.0
 * @description 轮廓间距
 * @date 2025/5/28 09:13:09
 */
@Data
public class AnnotationDistanceReq {

    @NotNull(message = "{NO_ANNOTATION_DATA}")
    @Schema(description = "标注id", required = true)
    private Long annotationIdOne;

    @NotNull(message = "{NO_ANNOTATION_DATA}")
    @Schema(description = "标注id", required = true)
    private Long annotationIdTwo;

    @NotNull(message = "{NO_ANNOTATION_DATA}")
    @Schema(description = "标注类型", required = true)
    private String annotationTypeOne;

    @NotNull(message = "{NO_ANNOTATION_DATA}")
    @Schema(description = "标注类型", required = true)
    private String annotationTypeTwo;

}

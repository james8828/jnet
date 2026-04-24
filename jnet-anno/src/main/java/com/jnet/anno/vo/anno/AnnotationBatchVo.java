package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标注批量响应 VO
 *
 * @author JNet Team
 * @since 2025-05-22
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AnnotationBatchVo {

    @JsonProperty("marking_id")
    private String annotationId;

    @JsonProperty("front_id")
    private String frontId;

    @Schema(description = "操作是否成功")
    private Boolean status = true;

    private String message;


}

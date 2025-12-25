package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mugw
 * @version 1.0
 * @description 新增标注参数
 * @date 2025/5/22 09:40:28
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AnnotationBatchRespVo {

    @JsonProperty("marking_id")
    private String annotationId;

    @JsonProperty("front_id")
    private String frontId;

    @Schema(description = "操作是否成功")
    private Boolean status = true;

    private String message;


}

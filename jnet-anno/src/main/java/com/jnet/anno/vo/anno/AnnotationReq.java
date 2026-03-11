package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author mugw
 * @version 1.0
 * @description 新增标注参数
 * @date 2025/5/22 09:40:28
 */
@Data
public class AnnotationReq {

    @NotNull(message = "{NO_SLIDE_DATA}")
    private Long slideId;
}

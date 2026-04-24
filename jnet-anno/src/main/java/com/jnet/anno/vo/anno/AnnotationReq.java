package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标注查询请求
 *
 * @author JNet Team
 * @since 2025-05-22
 */
@Data
public class AnnotationReq {

    @NotNull(message = "{NO_SLIDE_DATA}")
    private Long slideId;
}

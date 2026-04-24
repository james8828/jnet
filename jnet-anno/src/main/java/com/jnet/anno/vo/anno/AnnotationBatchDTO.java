package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 标注批量请求
 *
 * @author JNet Team
 * @since 2025-04-16
 */
@Data
public class AnnotationBatchDTO {

    @NotNull(message = "{NO_SLIDE_DATA}")
    @JsonProperty("slide_id")
    private Long slideId;

    List<AnnotationOperation> list;
}

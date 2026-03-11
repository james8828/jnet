package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author .
 */
@Data
public class AnnotationBatchReq{

    @NotNull(message = "{NO_SLIDE_DATA}")
    @JsonProperty("slide_id")
    private Long slideId;

    List<AnnotationBatchVo> list;
}

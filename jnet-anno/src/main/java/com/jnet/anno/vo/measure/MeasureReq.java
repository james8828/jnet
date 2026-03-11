package com.jnet.anno.vo.measure;

import com.jnet.api.vo.PageQueryVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/30 09:25:09
 */
@Data
public class MeasureReq extends PageQueryVO {

    @NotNull(message = "{ARGUMENT_INVALID}")
    @Schema(description = "切片ID")
    private Long slideId;

    @Schema(description = "标注名称")
    private String measureFullName;
}

package com.jnet.anno.vo.measure;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 测量查询请求
 *
 * @author JNet Team
 * @since 2025-05-30
 */
@Data
public class MeasureReq {

    @NotNull(message = "{ARGUMENT_INVALID}")
    @Schema(description = "切片ID")
    private Long slideId;

    @Schema(description = "标注名称")
    private String measureFullName;

    private Integer current;

    private Integer size;
}

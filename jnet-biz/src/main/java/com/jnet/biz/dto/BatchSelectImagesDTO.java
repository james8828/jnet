package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量选择切片DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "批量选择切片请求")
public class BatchSelectImagesDTO {

    @NotEmpty(message = "图像ID列表不能为空")
    @Schema(description = "图像ID列表", example = "[1, 2, 3, 4, 5]", required = true)
    private List<Long> imageIds;

    @NotNull(message = "目标批次ID不能为空")
    @Schema(description = "目标批次ID", example = "1", required = true)
    private Long targetBatchId;

    @Schema(description = "操作类型: MOVE-移动, COPY-复制", example = "COPY")
    private String operationType = "COPY";
}

package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 复制图像DTO
 *
 * @author JNet Team
 * @since 2024-05-09
 */
@Data
@Schema(description = "复制图像请求")
public class CopyImageDTO {

    @NotEmpty(message = "图像ID列表不能为空")
    @Schema(description = "要复制的图像ID列表", example = "[1, 2, 3, 4, 5]", required = true)
    private List<Long> imageIds;

    @NotNull(message = "目标文件夹ID不能为空")
    @Schema(description = "目标文件夹（批次）ID", example = "1", required = true)
    private Long targetBatchId;
}

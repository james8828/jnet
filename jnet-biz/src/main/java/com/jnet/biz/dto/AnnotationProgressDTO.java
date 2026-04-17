package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量更新标注进度 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "批量更新标注进度请求")
public class AnnotationProgressDTO {

    /**
     * 图像ID列表
     */
    @NotEmpty(message = "图像ID列表不能为空")
    @Schema(description = "图像ID列表", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> imageIds;

    /**
     * 标注进度 (0-100)
     */
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    @Schema(description = "标注进度（0-100）", example = "75", minimum = "0", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer progress;
}

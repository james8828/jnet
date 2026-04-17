package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量打标 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "批量打标请求")
public class BatchAssignTagsDTO {

    /**
     * 资产ID列表
     */
    @NotEmpty(message = "资产ID列表不能为空")
    @Schema(description = "资产ID列表", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> assetIds;

    /**
     * 标签ID列表
     */
    @NotEmpty(message = "标签ID列表不能为空")
    @Schema(description = "标签ID列表", example = "[10, 20]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> tagIds;
}

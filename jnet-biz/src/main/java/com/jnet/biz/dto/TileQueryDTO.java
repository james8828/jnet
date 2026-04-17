package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Tile查询DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "Tile查询参数")
public class TileQueryDTO {

    @NotNull(message = "图像ID不能为空")
    @Schema(description = "图像ID", example = "1", required = true)
    private Long imageId;

    @Min(value = 0, message = "层级不能为负数")
    @Schema(description = "缩放层级（0为最高分辨率）", example = "0", required = true)
    private Integer level;

    @Min(value = 0, message = "列索引不能为负数")
    @Schema(description = "Tile列索引", example = "0", required = true)
    private Integer col;

    @Min(value = 0, message = "行索引不能为负数")
    @Schema(description = "Tile行索引", example = "0", required = true)
    private Integer row;

    @Schema(description = "Tile宽度（像素）", example = "256")
    private Integer tileSize = 256;
}

package com.jnet.biz.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图像元数据VO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图像元数据信息")
public class ImageMetadataVO {

    @Schema(description = "图像ID", example = "1")
    private Long imageId;

    @Schema(description = "文件名", example = "R19-219-RD 20-1346-12 1M.svs")
    private String filename;

    @Schema(description = "图像宽度（像素）", example = "100000")
    private Integer width;

    @Schema(description = "图像高度（像素）", example = "80000")
    private Integer height;

    @Schema(description = "金字塔层级数", example = "8")
    private Integer levelCount;

    @Schema(description = "每层级的尺寸 [{width, height}, ...]")
    private java.util.List<int[]> levelDimensions;

    @Schema(description = "X轴物理分辨率 (um/px)", example = "0.25")
    private Double mppX;

    @Schema(description = "Y轴物理分辨率 (um/px)", example = "0.25")
    private Double mppY;

    @Schema(description = "放大倍数", example = "40")
    private Integer magnification;

    @Schema(description = "Tile宽度", example = "256")
    private Integer tileWidth;

    @Schema(description = "Tile高度", example = "256")
    private Integer tileHeight;

    @Schema(description = "文件格式", example = "SVS")
    private String format;

    @Schema(description = "缩略图Base64（可选）")
    private String thumbnailBase64;
}

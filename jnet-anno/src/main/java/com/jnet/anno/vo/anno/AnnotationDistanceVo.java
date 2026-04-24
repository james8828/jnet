package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

/**
 * 标注距离 VO
 *
 * @author JNet Team
 * @since 2025-05-28
 */
@Data
public class AnnotationDistanceVo {

    @Schema(description = "轮廓点一")
    @JsonProperty("contourTypeOne")
    private Geometry pointOne;

    @Schema(description = "轮廓点二")
    @JsonProperty("contourTypeTwo")
    private Geometry pointTwo;

    @Schema(description = "平均间距")
    private Double meanDistance;

    @Schema(description = "最小间距")
    private Double minDistance;
}

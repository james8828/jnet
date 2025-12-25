package com.jnet.anno.vo.anno;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

/**
 * @author mugw
 * @version 1.0
 * @description 轮廓间距
 * @date 2025/5/28 09:13:09
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

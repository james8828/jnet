package com.jnet.anno.vo.anno;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

/**
 * 标注批量 VO
 *
 * @author JNet Team
 * @since 2025-05-22
 */
@Data
public class AnnotationOperation {

    /**
     * 主键id
     */
    private Long annotationId;

    /**
     * 轮廓
     */
    private Geometry geom;


    @NotNull(message = "{NO_SLIDE_DATA}")
    @Schema(description = "操作：修改-UPDATE,删除-DELETE,添加-INSERT")
    private String operation;


}

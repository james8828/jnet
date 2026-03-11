package com.jnet.anno.netty.message;

import lombok.Data;
import org.locationtech.jts.geom.Geometry;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/22 13:50:42
 */
@Data
public class AnnotationFeature {

    private String id;

    private String type = "Feature";

    private Geometry geometry;

    private AnnotationProperties properties;

}

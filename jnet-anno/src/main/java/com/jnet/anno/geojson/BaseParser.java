package com.jnet.anno.geojson;

import org.locationtech.jts.geom.GeometryFactory;

/**
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class BaseParser {

    public static final String TYPE = "type";
    public static final String GEOMETRIES = "geometries";
    public static final String COORDINATES = "coordinates";

    protected GeometryFactory geometryFactory;

    public BaseParser(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

}

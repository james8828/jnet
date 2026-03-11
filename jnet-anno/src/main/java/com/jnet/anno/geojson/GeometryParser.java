package com.jnet.anno.geojson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;

/**
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public interface GeometryParser<T extends Geometry> {

    T geometryFromJson(JsonNode node) throws JsonMappingException;

}

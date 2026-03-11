package com.jnet.common.geo.jackson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/21 10:19:32
 */
public interface GeometryParser<T extends Geometry> {

    T geometryFromJson(JsonNode node) throws JsonMappingException;

}

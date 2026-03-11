package com.jnet.anno.geojson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jnet.anno.geojson.BaseParser;
import com.jnet.anno.geojson.GeometryParser;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;


/**
 * MultiPoint 解析器，将 GeoJSON 多点坐标转换为 MultiPoint 对象
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class MultiPointParser extends BaseParser implements GeometryParser<MultiPoint> {

    public MultiPointParser(GeometryFactory geometryFactory) {
        super(geometryFactory);

        if (geometryFactory == null) {
            throw new IllegalArgumentException("GeometryFactory cannot be null");
        }
    }

    public MultiPoint multiPointFromJson(JsonNode root) throws JsonMappingException {
        if (root == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }

        JsonNode coordinatesNode = root.get(COORDINATES);
        if (coordinatesNode == null || coordinatesNode.isMissingNode()) {
            throw new JsonMappingException(
                null,
                "Missing 'coordinates' field in MultiPoint"
            );
        }

        if (!coordinatesNode.isArray()) {
            throw new JsonMappingException(
                null,
                "Expected array of points, but got: " + coordinatesNode.getNodeType()
            );
        }

        return geometryFactory.createMultiPoint(
            PointParser.coordinatesFromJson(coordinatesNode)
        );
    }

    @Override
    public MultiPoint geometryFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }
        return multiPointFromJson(node);
    }
}

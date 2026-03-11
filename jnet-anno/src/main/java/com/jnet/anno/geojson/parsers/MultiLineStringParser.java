package com.jnet.anno.geojson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jnet.anno.geojson.BaseParser;
import com.jnet.anno.geojson.GeometryParser;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

/**
 * MultiLineString 解析器，将 GeoJSON 多线串坐标转换为 MultiLineString 对象
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class MultiLineStringParser extends BaseParser implements GeometryParser<MultiLineString> {

    public MultiLineStringParser(GeometryFactory geometryFactory) {
        super(geometryFactory);

        if (geometryFactory == null) {
            throw new IllegalArgumentException("GeometryFactory cannot be null");
        }
    }

    public MultiLineString multiLineStringFromJson(JsonNode root) throws JsonMappingException {
        if (root == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }

        JsonNode coordinatesNode = root.get(COORDINATES);
        if (coordinatesNode == null || coordinatesNode.isMissingNode()) {
            throw new JsonMappingException(
                null,
                "Missing 'coordinates' field in MultiLineString"
            );
        }

        if (!coordinatesNode.isArray()) {
            throw new JsonMappingException(
                null,
                "Expected array of line strings, but got: " + coordinatesNode.getNodeType()
            );
        }

        return geometryFactory.createMultiLineString(
            lineStringsFromJson(coordinatesNode)
        );
    }

    private LineString[] lineStringsFromJson(JsonNode array) throws JsonMappingException {
        if (array == null || array.isMissingNode()) {
            throw new JsonMappingException(null, "LineString array is null or missing");
        }

        int size = array.size();
        LineString[] strings = new LineString[size];

        for (int i = 0; i < size; i++) {
            JsonNode lineStringCoords = array.get(i);
            if (lineStringCoords == null || lineStringCoords.isNull()) {
                throw new JsonMappingException(
                    null,
                    "LineString at index " + i + " is null"
                );
            }

            if (!lineStringCoords.isArray()) {
                throw new JsonMappingException(
                    null,
                    "Expected array of coordinates at index " + i + ", but got: " + lineStringCoords.getNodeType()
                );
            }

            strings[i] = geometryFactory.createLineString(
                PointParser.coordinatesFromJson(lineStringCoords)
            );
        }

        return strings;
    }

    @Override
    public MultiLineString geometryFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }
        return multiLineStringFromJson(node);
    }
}

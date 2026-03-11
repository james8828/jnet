package com.jnet.anno.geojson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jnet.anno.geojson.BaseParser;
import com.jnet.anno.geojson.GeometryParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * Point 解析器，将 GeoJSON 点坐标转换为 Point 对象
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class PointParser extends BaseParser implements GeometryParser<Point> {

    public PointParser(GeometryFactory geometryFactory) {
        super(geometryFactory);

        if (geometryFactory == null) {
            throw new IllegalArgumentException("GeometryFactory cannot be null");
        }
    }

    public static Coordinate coordinateFromJson(JsonNode array) throws JsonMappingException {
        if (array == null || !array.isArray()) {
            throw new JsonMappingException(
                null,
                "Expected coordinate array [x, y] or [x, y, z], but got: " +
                (array == null ? "null" : array.getNodeType().toString())
            );
        }

        int size = array.size();
        if (size != 2 && size != 3) {
            throw new JsonMappingException(
                null,
                "Expected coordinate array with 2 or 3 elements, but got: " + size
            );
        }

        JsonNode xNode = array.get(0);
        JsonNode yNode = array.get(1);

        if (xNode == null || yNode == null) {
            throw new JsonMappingException(null, "Coordinate x and y values cannot be null");
        }

        double x = xNode.asDouble();
        double y = yNode.asDouble();

        if (size == 2) {
            return new Coordinate(x, y);
        }

        JsonNode zNode = array.get(2);
        if (zNode == null) {
            throw new JsonMappingException(null, "Coordinate z value cannot be null when array size is 3");
        }

        double z = zNode.asDouble();
        return new Coordinate(x, y, z);
    }

    public static Coordinate[] coordinatesFromJson(JsonNode array) throws JsonMappingException {
        if (array == null || !array.isArray()) {
            throw new JsonMappingException(
                null,
                "Expected array of coordinates, but got: " +
                (array == null ? "null" : array.getNodeType().toString())
            );
        }

        int size = array.size();
       Coordinate[] points = new Coordinate[size];

        for (int i = 0; i < size; i++) {
            JsonNode coordNode = array.get(i);
            if (coordNode == null || coordNode.isNull()) {
                throw new JsonMappingException(
                    null,
                    "Coordinate at index " + i + " is null"
                );
            }
            points[i] = coordinateFromJson(coordNode);
        }

        return points;
    }

    public Point pointFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }

        JsonNode coordinatesNode = node.get(COORDINATES);
        if (coordinatesNode == null || coordinatesNode.isMissingNode()) {
            throw new JsonMappingException(
                null,
                "Missing 'coordinates' field in Point"
            );
        }

        return geometryFactory.createPoint(
            coordinateFromJson(coordinatesNode)
        );
    }

    @Override
    public Point geometryFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }
        return pointFromJson(node);
    }
}

package com.jnet.anno.geojson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jnet.anno.geojson.BaseParser;
import com.jnet.anno.geojson.GeometryParser;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * Polygon 解析器，将 GeoJSON 多边形坐标（包含外环和内环）转换为 Polygon 对象
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class PolygonParser extends BaseParser implements GeometryParser<Polygon> {

    public PolygonParser(GeometryFactory geometryFactory) {
        super(geometryFactory);

        if (geometryFactory == null) {
            throw new IllegalArgumentException("GeometryFactory cannot be null");
        }
    }

    public Polygon polygonFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }

        JsonNode arrayOfRings = node.get(COORDINATES);
        if (arrayOfRings == null || arrayOfRings.isMissingNode()) {
            throw new JsonMappingException(
                    null,
                    "Missing 'coordinates' field in Polygon"
            );
        }

        return polygonFromJsonArrayOfRings(arrayOfRings);
    }

    public Polygon polygonFromJsonArrayOfRings(JsonNode arrayOfRings) throws JsonMappingException {
        // 验证输入参数
        if (arrayOfRings == null || arrayOfRings.isMissingNode()) {
            throw new JsonMappingException(null, "Input arrayOfRings cannot be null");
        }

        if (!arrayOfRings.isArray()) {
            throw new JsonMappingException(
                    null,
                    "Expected array of rings, but got: " + arrayOfRings.getNodeType()
            );
        }

        // 验证数组是否至少包含一个元素（shell）
        int size = arrayOfRings.size();
        if (size < 1) {
            throw new JsonMappingException(
                    null,
                    "Array of rings must contain at least one element for shell"
            );
        }

        // 获取并验证 shell
        JsonNode shellNode = arrayOfRings.get(0);
        if (shellNode == null || shellNode.isNull()) {
            throw new JsonMappingException(null, "Shell ring cannot be null");
        }

        if (!shellNode.isArray()) {
            throw new JsonMappingException(
                    null,
                    "Expected shell to be an array, but got: " + shellNode.getNodeType()
            );
        }

        LinearRing shell = linearRingsFromJson(shellNode);

        LinearRing[] holes;
        if (size > 1) {
            holes = new LinearRing[size - 1];
            for (int i = 1; i < size; i++) {
                JsonNode holeNode = arrayOfRings.get(i);
                if (holeNode == null || holeNode.isNull()) {
                    throw new JsonMappingException(
                            null,
                            "Hole ring at index " + i + " cannot be null"
                    );
                }

                if (!holeNode.isArray()) {
                    throw new JsonMappingException(
                            null,
                            "Expected hole at index " + i + " to be an array, but got: " + holeNode.getNodeType()
                    );
                }

                holes[i - 1] = linearRingsFromJson(holeNode);
            }
        } else {
            holes = new LinearRing[0];
        }

        return geometryFactory.createPolygon(shell, holes);
    }


    private LinearRing linearRingsFromJson(JsonNode coordinates) throws JsonMappingException {
        if (coordinates == null || !coordinates.isArray()) {
            throw new JsonMappingException(
                    null,
                    "Expected coordinates array, but got: " +
                            (coordinates == null ? "null" : coordinates.getNodeType().toString())
            );
        }

        return geometryFactory.createLinearRing(
                PointParser.coordinatesFromJson(coordinates)
        );
    }


    @Override
    public Polygon geometryFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }
        return polygonFromJson(node);
    }
}

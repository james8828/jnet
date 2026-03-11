package com.jnet.common.geo.jackson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import static com.jnet.common.geo.jackson.GeoJson.COORDINATES;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/21 10:19:32
 */
public class PolygonParser extends BaseParser implements GeometryParser<Polygon> {

    public PolygonParser(GeometryFactory geometryFactory) {
        super(geometryFactory);
    }

    public Polygon polygonFromJson(JsonNode node) {
        JsonNode arrayOfRings = node.get(COORDINATES);
        return polygonFromJsonArrayOfRings(arrayOfRings);
    }

    public Polygon polygonFromJsonArrayOfRings(JsonNode arrayOfRings) {
        // 验证输入参数
        if (arrayOfRings == null) {
            throw new IllegalArgumentException("Input arrayOfRings cannot be null");
        }

        // 验证数组是否至少包含一个元素（shell）
        if (arrayOfRings.size() < 1) {
            throw new IllegalArgumentException("Array of rings must contain at least one element for shell");
        }

        // 获取并验证shell
        JsonNode shellNode = arrayOfRings.get(0);
        if (shellNode == null) {
            throw new IllegalArgumentException("Shell ring cannot be null");
        }
        LinearRing shell = linearRingsFromJson(shellNode);

        int size = arrayOfRings.size();
        LinearRing[] holes;
        if (size > 1) {
            holes = new LinearRing[size - 1];
            for (int i = 1; i < size; i++) {
                JsonNode holeNode = arrayOfRings.get(i);
                if (holeNode == null) {
                    throw new IllegalArgumentException("Hole ring at index " + i + " cannot be null");
                }
                holes[i - 1] = linearRingsFromJson(holeNode);
            }
        } else {
            holes = new LinearRing[0]; // 空洞数组，当没有holes时
        }

        return geometryFactory.createPolygon(shell, holes);
    }


    private LinearRing linearRingsFromJson(JsonNode coordinates) {
        assert coordinates.isArray() : "expected coordinates array";
        return geometryFactory.createLinearRing(PointParser.coordinatesFromJson(coordinates));
    }


    @Override
    public Polygon geometryFromJson(JsonNode node) throws JsonMappingException {
        return polygonFromJson(node);
    }
}

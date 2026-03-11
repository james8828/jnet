package com.jnet.anno.geojson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jnet.anno.geojson.BaseParser;
import com.jnet.anno.geojson.GeometryParser;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/**
 * MultiPolygon 解析器，将 GeoJSON 多多边形坐标转换为 MultiPolygon 对象
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class MultiPolygonParser extends BaseParser implements GeometryParser<MultiPolygon> {

    private final PolygonParser polygonParser;

    public MultiPolygonParser(GeometryFactory geometryFactory) {
        super(geometryFactory);

        if (geometryFactory == null) {
            throw new IllegalArgumentException("GeometryFactory cannot be null");
        }

        this.polygonParser = new PolygonParser(geometryFactory);
    }

    public MultiPolygon multiPolygonFromJson(JsonNode root) throws JsonMappingException {
        if (root == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }

        JsonNode arrayOfPolygons = root.get(COORDINATES);
        if (arrayOfPolygons == null || arrayOfPolygons.isMissingNode()) {
            throw new JsonMappingException(
                null,
                "Missing 'coordinates' field in MultiPolygon"
            );
        }

        if (!arrayOfPolygons.isArray()) {
            throw new JsonMappingException(
                null,
                "Expected array of polygons, but got: " + arrayOfPolygons.getNodeType()
            );
        }

        return geometryFactory.createMultiPolygon(polygonsFromJson(arrayOfPolygons));
    }

    private Polygon[] polygonsFromJson(JsonNode arrayOfPolygons) throws JsonMappingException {
        if (arrayOfPolygons == null || arrayOfPolygons.isMissingNode()) {
            throw new JsonMappingException(null, "Polygon array is null or missing");
        }

        int size = arrayOfPolygons.size();
        Polygon[] polygons = new Polygon[size];

        for (int i = 0; i < size; i++) {
            JsonNode polygonCoords = arrayOfPolygons.get(i);
            if (polygonCoords == null || polygonCoords.isNull()) {
                throw new JsonMappingException(
                    null,
                    "Polygon at index " + i + " is null"
                );
            }

            if (!polygonCoords.isArray()) {
                throw new JsonMappingException(
                    null,
                    "Expected array of rings at index " + i + ", but got: " + polygonCoords.getNodeType()
                );
            }

            polygons[i] = polygonParser.polygonFromJsonArrayOfRings(polygonCoords);
        }

        return polygons;
    }

    @Override
    public MultiPolygon geometryFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }
        return multiPolygonFromJson(node);
    }
}

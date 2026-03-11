package com.jnet.anno.geojson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jnet.anno.geojson.parsers.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * 通用 Geometry 解析器，根据 GeoJSON type 字段分发到具体的解析器
 *
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class GenericGeometryParser extends BaseParser implements GeometryParser<Geometry> {

    private final Map<String, GeometryParser<?>> parsers;

    public GenericGeometryParser(GeometryFactory geometryFactory) {
        super(geometryFactory);

        if (geometryFactory == null) {
            throw new IllegalArgumentException("GeometryFactory cannot be null");
        }

        Map<String, GeometryParser<?>> parserMap = new HashMap<>(8);
        parserMap.put(Geometry.TYPENAME_POINT, new PointParser(geometryFactory));
        parserMap.put(Geometry.TYPENAME_MULTIPOINT, new MultiPointParser(geometryFactory));
        parserMap.put(Geometry.TYPENAME_LINESTRING, new LineStringParser(geometryFactory));
        parserMap.put(Geometry.TYPENAME_MULTILINESTRING, new MultiLineStringParser(geometryFactory));
        parserMap.put(Geometry.TYPENAME_POLYGON, new PolygonParser(geometryFactory));
        parserMap.put(Geometry.TYPENAME_MULTIPOLYGON, new MultiPolygonParser(geometryFactory));
        parserMap.put(Geometry.TYPENAME_GEOMETRYCOLLECTION, new GeometryCollectionParser(geometryFactory, this));

        this.parsers = Collections.unmodifiableMap(parserMap);
    }

    @Override
    public Geometry geometryFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }

        JsonNode typeNode = node.get(TYPE);
        if (typeNode == null || typeNode.isMissingNode()) {
            throw new JsonMappingException(
                    null,
                    "Missing 'type' field in geometry JSON. Supported types are: " + parsers.keySet()
            );
        }

        String typeName = typeNode.asText();
        GeometryParser<?> parser = parsers.get(typeName);

        if (parser != null) {
            return parser.geometryFromJson(node);
        } else {
            throw new JsonMappingException(
                    null,
                    "Invalid geometry type: '" + typeName + "'. Supported types are: " + parsers.keySet()
            );
        }
    }
}

package com.jnet.anno.geojson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jnet.anno.geojson.BaseParser;
import com.jnet.anno.geojson.GenericGeometryParser;
import com.jnet.anno.geojson.GeometryParser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;


/**
 * GeometryCollection 解析器，将 GeoJSON 几何集合数组转换为 GeometryCollection 对象
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class GeometryCollectionParser extends BaseParser implements GeometryParser<GeometryCollection> {

    private final GenericGeometryParser genericGeometryParser;

    public GeometryCollectionParser(GeometryFactory geometryFactory, GenericGeometryParser genericGeometryParser) {
        super(geometryFactory);

        if (genericGeometryParser == null) {
            throw new IllegalArgumentException("GenericGeometryParser cannot be null");
        }

        this.genericGeometryParser = genericGeometryParser;
    }

    private Geometry[] geometriesFromJson(JsonNode arrayOfGeoms) throws JsonMappingException {
        if (arrayOfGeoms == null || arrayOfGeoms.isMissingNode()) {
            throw new JsonMappingException(null, "Geometries array is missing or null");
        }

        if (!arrayOfGeoms.isArray()) {
            throw new JsonMappingException(
                null,
                "Expected array of geometries, but got: " + arrayOfGeoms.getNodeType()
            );
        }

        int size = arrayOfGeoms.size();
        Geometry[] items = new Geometry[size];

        for (int i = 0; i < size; i++) {
            JsonNode geometryNode = arrayOfGeoms.get(i);
            if (geometryNode == null || geometryNode.isNull()) {
                throw new JsonMappingException(
                    null,
                    "Geometry at index " + i + " is null"
                );
            }
            items[i] = genericGeometryParser.geometryFromJson(geometryNode);
        }

        return items;
    }

    @Override
    public GeometryCollection geometryFromJson(JsonNode node) throws JsonMappingException {
        if (node == null) {
            throw new JsonMappingException(null, "JsonNode cannot be null");
        }

        JsonNode geometriesNode = node.get(GEOMETRIES);
        if (geometriesNode == null || geometriesNode.isMissingNode()) {
            throw new JsonMappingException(
                null,
                "Missing 'geometries' field in GeometryCollection"
            );
        }

        return geometryFactory.createGeometryCollection(geometriesFromJson(geometriesNode));
    }
}

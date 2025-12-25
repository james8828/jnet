package com.jnet.common.geo.jackson.parsers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import com.jnet.common.geo.jackson.GeoJson;

import java.util.HashMap;
import java.util.Map;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/21 10:19:32
 */
public class GenericGeometryParser extends BaseParser implements GeometryParser<Geometry> {

    private Map<String, GeometryParser> parsers;

    public GenericGeometryParser(GeometryFactory geometryFactory) {
        super(geometryFactory);
        parsers = new HashMap<String, GeometryParser>();
        parsers.put(Geometry.TYPENAME_POINT, new PointParser(geometryFactory));
        parsers.put(Geometry.TYPENAME_MULTIPOINT, new MultiPointParser(geometryFactory));
        parsers.put(Geometry.TYPENAME_LINESTRING, new LineStringParser(geometryFactory));
        parsers.put(Geometry.TYPENAME_MULTILINESTRING, new MultiLineStringParser(geometryFactory));
        parsers.put(Geometry.TYPENAME_POLYGON, new PolygonParser(geometryFactory));
        parsers.put(Geometry.TYPENAME_MULTIPOLYGON, new MultiPolygonParser(geometryFactory));
        parsers.put(Geometry.TYPENAME_GEOMETRYCOLLECTION, new GeometryCollectionParser(geometryFactory, this));
    }

    @Override
    public Geometry geometryFromJson(JsonNode node) throws JsonMappingException {
        String typeName = node.get(GeoJson.TYPE).asText();
        GeometryParser parser = parsers.get(typeName);
        if (parser != null) {
            return parser.geometryFromJson(node);
        }
        else {
            throw new JsonMappingException("Invalid geometry type: " + typeName);
        }
    }
}

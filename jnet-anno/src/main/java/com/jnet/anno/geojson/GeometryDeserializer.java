package com.jnet.anno.geojson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;

/**
 * GeoJSON 反序列化器，将 JSON 转换为 Geometry 对象
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class GeometryDeserializer<T extends Geometry> extends JsonDeserializer<T> {

    private final GeometryParser<T> geometryParser;

    public GeometryDeserializer(GeometryParser<T> geometryParser) {
        if (geometryParser == null) {
            throw new IllegalArgumentException("GeometryParser cannot be null");
        }
        this.geometryParser = geometryParser;
    }

    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (jsonParser == null) {
            throw new JsonMappingException(null, "JsonParser cannot be null");
        }
        ObjectCodec oc = jsonParser.getCodec();
        if (oc == null) {
            throw new JsonMappingException(jsonParser, "ObjectCodec is not set in JsonParser");
        }

        JsonNode root = oc.readTree(jsonParser);

        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }

        try {
            return geometryParser.geometryFromJson(root);
        } catch (Exception e) {
            throw new JsonMappingException(
                jsonParser,
                "Failed to parse Geometry from JSON: " + e.getMessage(),
                e
            );
        }
    }
}

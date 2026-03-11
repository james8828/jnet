package com.jnet.anno.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.locationtech.jts.geom.*;
import java.io.IOException;
import java.util.Arrays;

/**
 * geometry serializer
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class GeometrySerializer extends JsonSerializer<Geometry> {

	public static final String TYPE = "type";
	public static final String GEOMETRIES = "geometries";
	public static final String COORDINATES = "coordinates";
	
	@Override
	public void serialize(Geometry value, JsonGenerator jgen,
						  SerializerProvider provider) throws IOException {

		writeGeometry(jgen, value);
	}

	public void writeGeometry(JsonGenerator jgen, Geometry value)
			throws IOException {
		if (value == null) {
			jgen.writeNull();
			return;
		}

		if (value instanceof Polygon polygon) {
			writePolygon(jgen, polygon);

		} else if (value instanceof Point point) {
			writePoint(jgen, point);

		} else if (value instanceof MultiPoint multiPoint) {
			writeMultiPoint(jgen, multiPoint);

		} else if (value instanceof MultiPolygon multiPolygon) {
			writeMultiPolygon(jgen, multiPolygon);

		} else if (value instanceof LineString lineString) {
			writeLineString(jgen, lineString);

		} else if (value instanceof MultiLineString multiLineString) {
			writeMultiLineString(jgen, multiLineString);

		} else if (value instanceof GeometryCollection geometryCollection) {
			writeGeometryCollection(jgen, geometryCollection);

		} else {
			throw new JsonMappingException(null, "Geometry type"
					+ value.getClass().getName() + " cannot be serialized as  " +
					"Supported types are: " + Arrays.asList(
						Point.class.getName(),
						LineString.class.getName(),
						Polygon.class.getName(),
						MultiPoint.class.getName(),
						MultiLineString.class.getName(),
						MultiPolygon.class.getName(),
						GeometryCollection.class.getName()));
		}
	}

	private void writeGeometryCollection(JsonGenerator jgen, GeometryCollection value) throws
			IOException {
		jgen.writeStartObject();
		jgen.writeStringField(TYPE, Geometry.TYPENAME_GEOMETRYCOLLECTION);
		jgen.writeArrayFieldStart(GEOMETRIES);

		for (int i = 0; i < value.getNumGeometries(); i++) {
			writeGeometry(jgen, value.getGeometryN(i));
		}

		jgen.writeEndArray();
		jgen.writeEndObject();
	}

	private void writeMultiPoint(JsonGenerator jgen, MultiPoint value)
			throws IOException {
		jgen.writeStartObject();
		jgen.writeStringField(TYPE, Geometry.TYPENAME_MULTIPOINT);
		jgen.writeArrayFieldStart(COORDINATES);

		for (int i = 0; i < value.getNumGeometries(); i++) {
			writePointCoords(jgen, (Point) value.getGeometryN(i));
		}

		jgen.writeEndArray();
		jgen.writeEndObject();
	}

	private void writeMultiLineString(JsonGenerator jgen, MultiLineString value)
			throws IOException {
		jgen.writeStartObject();
		jgen.writeStringField(TYPE, Geometry.TYPENAME_MULTILINESTRING);
		jgen.writeArrayFieldStart(COORDINATES);

		for (int i = 0; i < value.getNumGeometries(); i++) {
			writeLineStringCoords(jgen, (LineString) value.getGeometryN(i));
		}

		jgen.writeEndArray();
		jgen.writeEndObject();
	}

	@Override
	public Class<Geometry> handledType() {
		return Geometry.class;
	}

	private void writeMultiPolygon(JsonGenerator jgen, MultiPolygon value)
			throws IOException {
		jgen.writeStartObject();
		jgen.writeStringField(TYPE, Geometry.TYPENAME_MULTIPOLYGON);
		jgen.writeArrayFieldStart(COORDINATES);

		for (int i = 0; i < value.getNumGeometries(); i++) {
			writePolygonCoordinates(jgen, (Polygon) value.getGeometryN(i));
		}

		jgen.writeEndArray();
		jgen.writeEndObject();
	}

	private void writePolygon(JsonGenerator jgen, Polygon value)
			throws IOException {
		jgen.writeStartObject();
		jgen.writeStringField(TYPE, Geometry.TYPENAME_POLYGON);
		jgen.writeFieldName(COORDINATES);
		writePolygonCoordinates(jgen, value);

		jgen.writeEndObject();
	}

	private void writePolygonCoordinates(JsonGenerator jgen, Polygon value)
			throws IOException {
		jgen.writeStartArray();
		writeLineStringCoords(jgen, value.getExteriorRing());

		for (int i = 0; i < value.getNumInteriorRing(); i++) {
			writeLineStringCoords(jgen, value.getInteriorRingN(i));
		}
		jgen.writeEndArray();
	}

	private void writeLineStringCoords(JsonGenerator jgen, LineString ring)
			throws IOException {
		jgen.writeStartArray();
		for (int i = 0; i < ring.getNumPoints(); i++) {
			Point p = ring.getPointN(i);
			writePointCoords(jgen, p);
		}
		jgen.writeEndArray();
	}

	private void writeLineString(JsonGenerator jgen, LineString lineString)
			throws IOException {
		jgen.writeStartObject();
		jgen.writeStringField(TYPE, Geometry.TYPENAME_LINESTRING);
		jgen.writeFieldName(COORDINATES);
		writeLineStringCoords(jgen, lineString);
		jgen.writeEndObject();
	}

	private void writePoint(JsonGenerator jgen, Point p)
			throws IOException {
		jgen.writeStartObject();
		jgen.writeStringField(TYPE, Geometry.TYPENAME_POINT);
		jgen.writeFieldName(COORDINATES);
		writePointCoords(jgen, p);
		jgen.writeEndObject();
	}

	private void writePointCoords(JsonGenerator jgen, Point p)
			throws IOException {
		jgen.writeStartArray();

		Coordinate coord = p.getCoordinate();
		if (coord == null) {
			jgen.writeNull();
			jgen.writeEndArray();
			return;
		}

		jgen.writeNumber(coord.x);
		jgen.writeNumber(coord.y);

		if (!Double.isNaN(coord.z)) {
			jgen.writeNumber(coord.z);
		}
		jgen.writeEndArray();
	}

}

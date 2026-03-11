package com.jnet.anno.geojson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jnet.anno.geojson.parsers.*;
import org.locationtech.jts.geom.*;

/**
 * Jackson JTS Module - 提供 GeoJSON 序列化/反序列化支持
 *
 * 该模块支持以下 GeoJSON 几何类型的序列化和反序列化：
 * <ul>
 *     <li>Point - 点</li>
 *     <li>MultiPoint- 多点</li>
 *     <li>LineString - 线串</li>
 *     <li>MultiLineString - 多线串</li>
 *     <li>Polygon - 多边形</li>
 *     <li>MultiPolygon- 多多边形</li>
 *     <li>GeometryCollection- 几何集合</li>
 * </ul>
 *
 * @author mugw
 * @version 1.0
 * @since 2026/3/10
 */
public class JtsModule extends SimpleModule {

    private static final String MODULE_NAME = "JtsModule";
    private static final Version MODULE_VERSION = new Version(1, 1, 0, null, "com.jnet", "jackson-datatype-jts");

    public JtsModule() {
        this(new GeometryFactory());
    }

    public JtsModule(GeometryFactory geometryFactory) {
        super(MODULE_NAME, MODULE_VERSION);

        if (geometryFactory == null) {
            throw new IllegalArgumentException("GeometryFactory cannot be null");
        }

        registerSerializers();
        registerDeserializers(geometryFactory);
    }

    private void registerSerializers() {
        addSerializer(Geometry.class, new GeometrySerializer());
    }

    private void registerDeserializers(GeometryFactory geometryFactory) {
        GenericGeometryParser genericGeometryParser = new GenericGeometryParser(geometryFactory);

        addDeserializer(Geometry.class, new GeometryDeserializer<>(genericGeometryParser));
        addDeserializer(Point.class, new GeometryDeserializer<>(new PointParser(geometryFactory)));
        addDeserializer(MultiPoint.class, new GeometryDeserializer<>(new MultiPointParser(geometryFactory)));
        addDeserializer(LineString.class, new GeometryDeserializer<>(new LineStringParser(geometryFactory)));
        addDeserializer(MultiLineString.class, new GeometryDeserializer<>(new MultiLineStringParser(geometryFactory)));
        addDeserializer(Polygon.class, new GeometryDeserializer<>(new PolygonParser(geometryFactory)));
        addDeserializer(MultiPolygon.class, new GeometryDeserializer<>(new MultiPolygonParser(geometryFactory)));
        addDeserializer(GeometryCollection.class, new GeometryDeserializer<>(new GeometryCollectionParser(geometryFactory, genericGeometryParser)));
    }
}

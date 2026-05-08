package com.jnet.anno.coco;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.locationtech.jts.geom.*;

import java.util.List;

/**
 * COCO 几何图形转换器
 * 将 COCO 格式的 bbox 和 segmentation 转换为 JTS Geometry 对象
 * 
 * @author jnet
 * @version 1.0
 * @since 2026/5/7
 */
public class CocoGeometryConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * 将 COCO bbox 转换为 Polygon
     * COCO bbox 格式: [x, y, width, height]
     *
     * @param bbox COCO 格式的边界框坐标列表
     * @return Polygon 几何对象，如果输入无效则返回 null
     */
    public static Polygon bboxToPolygon(List<Double> bbox) {
        if (bbox == null || bbox.size() != 4) {
            return null;
        }

        try {
            double x = bbox.get(0);
            double y = bbox.get(1);
            double width = bbox.get(2);
            double height = bbox.get(3);

            // 创建矩形的四个角点（逆时针方向）
            // 注意：将 Y 坐标取负，适配 OpenLayers Zoomify 坐标系
            Coordinate[] coordinates = new Coordinate[5];
            coordinates[0] = new Coordinate(x, -y);                      // 左下
            coordinates[1] = new Coordinate(x + width, -y);              // 右下
            coordinates[2] = new Coordinate(x + width, -(y + height));     // 右上
            coordinates[3] = new Coordinate(x, -(y + height));             // 左上
            coordinates[4] = new Coordinate(x, -y);                      // 闭合

            LinearRing shell = geometryFactory.createLinearRing(coordinates);
            return geometryFactory.createPolygon(shell);
        } catch (Exception e) {
            System.err.println("转换 bbox 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将 COCO segmentation 转换为 Geometry
     * COCO segmentation 有两种格式:
     * 1. RLE 格式 (iscrowd=1): 暂不支持，返回 null
     * 2. 多边形格式 (iscrowd=0): List<List<Double>>，每个子列表是一个多边形的坐标序列 [x1,y1,x2,y2,...,xn,yn]
     *
     * @param segmentation COCO 格式的分割数据
     * @param iscrowd 是否为 crowd 标注
     * @return Geometry 对象（可能是 Polygon 或 MultiPolygon），如果转换失败则返回 null
     */
    public static Geometry segmentationToGeometry(List<List<Double>> segmentation, Integer iscrowd) {
        if (segmentation == null || segmentation.isEmpty()) {
            return null;
        }

        // 如果是 crowd 标注，segmentation 是 RLE 格式，暂不支持
        if (iscrowd != null && iscrowd == 1) {
            System.out.println("警告: RLE 格式的 segmentation 暂不支持转换 (iscrowd=1)");
            return null;
        }

        try {
            // 处理多边形格式
            if (segmentation.size() == 1) {
                // 单个多边形
                return polygonFromCoordinateList(segmentation.get(0));
            } else {
                // 多个多边形，创建 MultiPolygon
                Polygon[] polygons = new Polygon[segmentation.size()];
                int validCount = 0;
                
                for (int i = 0; i < segmentation.size(); i++) {
                    Polygon poly = polygonFromCoordinateList(segmentation.get(i));
                    if (poly != null) {
                        polygons[validCount++] = poly;
                    } else {
                        System.err.println("警告: 第 " + (i + 1) + " 个多边形转换失败");
                    }
                }
                
                if (validCount == 0) {
                    return null;
                } else if (validCount == 1) {
                    return polygons[0];
                } else {
                    // 只使用有效的多边形创建 MultiPolygon
                    Polygon[] validPolygons = new Polygon[validCount];
                    System.arraycopy(polygons, 0, validPolygons, 0, validCount);
                    return geometryFactory.createMultiPolygon(validPolygons);
                }
            }
        } catch (Exception e) {
            System.err.println("转换 segmentation 失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从坐标列表创建 Polygon
     * COCO 坐标格式: [x1, y1, x2, y2, ..., xn, yn]
     *
     * @param coordinates 扁平化的坐标列表
     * @return Polygon 对象，如果输入无效则返回 null
     */
    private static Polygon polygonFromCoordinateList(List<Double> coordinates) {
        if (coordinates == null || coordinates.size() < 6 || coordinates.size() % 2 != 0) {
            return null;
        }

        int numPoints = coordinates.size() / 2;
        Coordinate[] coords = new Coordinate[numPoints + 1];

        // 提取坐标点
        for (int i = 0; i < numPoints; i++) {
            double x = coordinates.get(i * 2);
            double y = coordinates.get(i * 2 + 1);
            // 将 Y 坐标取负，适配 OpenLayers Zoomify 坐标系
            coords[i] = new Coordinate(x, -y);
        }

        // 闭合多边形（首尾相连）
        coords[numPoints] = new Coordinate(coords[0].x, coords[0].y);

        LinearRing shell = geometryFactory.createLinearRing(coords);
        return geometryFactory.createPolygon(shell);
    }

    /**
     * 使用 JsonNode 方式从 COCO bbox 创建 Polygon
     * 适用于需要与现有 GeoJSON 解析器集成的场景
     *
     * @param bboxNode bbox 的 JsonNode 表示
     * @return Polygon 几何对象
     * @throws JsonMappingException 解析异常
     */
    public static Polygon bboxToPolygon(JsonNode bboxNode) throws JsonMappingException {
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() != 4) {
            throw new JsonMappingException(null, 
                "Invalid bbox format: expected array of 4 elements [x, y, width, height]");
        }

        double x = bboxNode.get(0).asDouble();
        double y = bboxNode.get(1).asDouble();
        double width = bboxNode.get(2).asDouble();
        double height = bboxNode.get(3).asDouble();

        Coordinate[] coordinates = new Coordinate[5];
        // 将 Y 坐标取负，适配 OpenLayers Zoomify 坐标系
        coordinates[0] = new Coordinate(x, -y);
        coordinates[1] = new Coordinate(x + width, -y);
        coordinates[2] = new Coordinate(x + width, -(y + height));
        coordinates[3] = new Coordinate(x, -(y + height));
        coordinates[4] = new Coordinate(x, -y);

        LinearRing shell = geometryFactory.createLinearRing(coordinates);
        return geometryFactory.createPolygon(shell);
    }

    /**
     * 使用 JsonNode 方式从 COCO segmentation 创建 Geometry
     * 适用于需要与现有 GeoJSON 解析器集成的场景
     *
     * @param segmentationNode segmentation 的 JsonNode 表示
     * @param iscrowd 是否为 crowd 标注
     * @return Geometry 对象
     * @throws JsonMappingException 解析异常
     */
    public static Geometry segmentationToGeometry(JsonNode segmentationNode, Integer iscrowd) 
            throws JsonMappingException {
        if (segmentationNode == null || !segmentationNode.isArray()) {
            throw new JsonMappingException(null, 
                "Invalid segmentation format: expected array of coordinate arrays");
        }

        if (iscrowd != null && iscrowd == 1) {
            throw new JsonMappingException(null, 
                "RLE format segmentation is not supported");
        }

        if (segmentationNode.size() == 0) {
            return null;
        }

        if (segmentationNode.size() == 1) {
            return polygonFromJsonArray(segmentationNode.get(0));
        } else {
            Polygon[] polygons = new Polygon[segmentationNode.size()];
            for (int i = 0; i < segmentationNode.size(); i++) {
                polygons[i] = polygonFromJsonArray(segmentationNode.get(i));
            }
            return geometryFactory.createMultiPolygon(polygons);
        }
    }

    /**
     * 从 JsonNode 数组创建 Polygon
     *
     * @param coordinatesNode 坐标数组节点 [x1, y1, x2, y2, ...]
     * @return Polygon 对象
     * @throws JsonMappingException 解析异常
     */
    private static Polygon polygonFromJsonArray(JsonNode coordinatesNode) throws JsonMappingException {
        if (coordinatesNode == null || !coordinatesNode.isArray()) {
            throw new JsonMappingException(null, 
                "Expected coordinate array [x1, y1, x2, y2, ...]");
        }

        int size = coordinatesNode.size();
        if (size < 6 || size % 2 != 0) {
            throw new JsonMappingException(null, 
                "Coordinate array must have at least 6 elements and be even-sized, got: " + size);
        }

        int numPoints = size / 2;
        Coordinate[] coords = new Coordinate[numPoints + 1];

        for (int i = 0; i < numPoints; i++) {
            double x = coordinatesNode.get(i * 2).asDouble();
            double y = coordinatesNode.get(i * 2 + 1).asDouble();
            // 将 Y 坐标取负，适配 OpenLayers Zoomify 坐标系
            coords[i] = new Coordinate(x, -y);
        }

        // 闭合多边形
        coords[numPoints] = new Coordinate(coords[0].x, coords[0].y);

        LinearRing shell = geometryFactory.createLinearRing(coords);
        return geometryFactory.createPolygon(shell);
    }

    /**
     * 验证 Geometry 是否有效
     *
     * @param geometry 要验证的几何对象
     * @return 是否有效
     */
    public static boolean isValid(Geometry geometry) {
        return geometry != null && geometry.isValid();
    }

    /**
     * 获取 Geometry 的 WKT 表示
     *
     * @param geometry 几何对象
     * @return WKT 字符串
     */
    public static String toWKT(Geometry geometry) {
        if (geometry == null) {
            return null;
        }
        return geometry.toText();
    }
}

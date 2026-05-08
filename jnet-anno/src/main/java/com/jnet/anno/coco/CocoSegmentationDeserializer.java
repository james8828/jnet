package com.jnet.anno.coco;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * COCO Segmentation 反序列化器
 * 处理两种格式：
 * 1. 多边形格式: [[x1,y1,x2,y2,...]]
 * 2. RLE 格式: {"counts": [...], "size": [h, w]}
 * 
 * @author jnet
 * @version 1.0
 * @since 2026/5/7
 */
public class CocoSegmentationDeserializer extends JsonDeserializer<List<List<Double>>> {

    @Override
    public List<List<Double>> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node == null || node.isNull()) {
            return null;
        }
        
        // 判断是数组格式（多边形）还是对象格式（RLE）
        if (node.isArray()) {
            // 多边形格式: [[x1,y1,x2,y2,...], ...]
            return deserializePolygonFormat(node);
        } else if (node.isObject()) {
            // RLE 格式: {"counts": [...], "size": [h, w]}
            // 暂时返回空列表，表示不支持 RLE 格式
            System.out.println("警告: 检测到 RLE 格式的 segmentation，暂不支持解析");
            return new ArrayList<>();
        } else {
            throw new IOException("Unsupported segmentation format: " + node.getNodeType());
        }
    }
    
    /**
     * 解析多边形格式的 segmentation
     */
    private List<List<Double>> deserializePolygonFormat(JsonNode node) {
        List<List<Double>> result = new ArrayList<>();
        
        for (JsonNode polygonNode : node) {
            if (polygonNode.isArray()) {
                List<Double> coordinates = new ArrayList<>();
                for (JsonNode coordNode : polygonNode) {
                    coordinates.add(coordNode.asDouble());
                }
                result.add(coordinates);
            }
        }
        
        return result;
    }
}

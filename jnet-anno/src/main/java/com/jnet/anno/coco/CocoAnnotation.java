package com.jnet.anno.coco;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

/**
 * COCO数据集标注实体
 */
@Data
public class CocoAnnotation {
    
    /**
     * 标注ID
     */
    private Long id;
    
    /**
     * 图片ID
     */
    @JsonProperty("image_id")
    private Long imageId;
    
    /**
     * 类别ID
     */
    @JsonProperty("category_id")
    private Long categoryId;
    
    /**
     * 边界框 [x, y, width, height] - 原始数据（用于JSON反序列化）
     */
    @JsonProperty("bbox")
    private List<Double> bboxRaw;
    
    /**
     * 边界框 - 转换后的 Geometry 对象（不参与JSON序列化）
     */
    @JsonIgnore
    private transient Geometry bbox;
    
    /**
     * 区域面积
     */
    private Double area;
    
    /**
     * 是否为crowd标注（0或1）
     */
    private Integer iscrowd;
    
    /**
     * 分割多边形坐标 - 原始数据（用于JSON反序列化）
     * 支持两种格式：
     * 1. 多边形格式: [[x1,y1,x2,y2,...]]
     * 2. RLE 格式: {"counts": [...], "size": [h, w]}（暂不支持，返回空列表）
     */
    @JsonProperty("segmentation")
    @JsonDeserialize(using = CocoSegmentationDeserializer.class)
    private List<List<Double>> segmentationRaw;
    
    /**
     * 分割多边形 - 转换后的 Geometry 对象（不参与JSON序列化）
     */
    @JsonIgnore
    private transient Geometry segmentation;
    
    /**
     * 将原始 bbox 转换为 Geometry
     * 应在 JSON 解析后调用
     */
    public void convertBboxToGeometry() {
        if (bboxRaw != null && !bboxRaw.isEmpty()) {
            this.bbox = CocoGeometryConverter.bboxToPolygon(bboxRaw);
        }
    }
    
    /**
     * 将原始 segmentation 转换为 Geometry
     * 应在 JSON 解析后调用
     */
    public void convertSegmentationToGeometry() {
        // 如果是 RLE 格式（空列表或 iscrowd=1），跳过转换
        if (segmentationRaw == null || segmentationRaw.isEmpty()) {
            return;
        }
        
        if (iscrowd != null && iscrowd == 1) {
            // RLE 格式，暂不支持
            return;
        }
        
        this.segmentation = CocoGeometryConverter.segmentationToGeometry(segmentationRaw, iscrowd);
    }
    
    /**
     * 转换所有几何字段
     * 应在 JSON 解析后调用
     */
    public void convertAllGeometries() {
        convertBboxToGeometry();
        convertSegmentationToGeometry();
    }
}

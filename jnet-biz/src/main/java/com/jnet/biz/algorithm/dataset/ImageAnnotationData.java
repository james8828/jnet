package com.jnet.biz.algorithm.dataset;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 图像标注数据
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class ImageAnnotationData {
    private Long imageId;
    private String filename;
    private String filePath;
    private Integer width;
    private Integer height;
    
    /**
     * 原始标注数据（算法特定的格式）
     * 例如：
     * - YOLO: List<String> ["0 0.5 0.5 0.2 0.3", "1 0.3 0.7 0.15 0.25"]
     * - COCO: List<Map<String, Object>> [{"category_id": 1, "bbox": [...]}]
     * - VOC: String (XML内容)
     * 
     * 使用 Object 类型以支持不同算法的标注格式
     */
    private Object rawLabels;
}

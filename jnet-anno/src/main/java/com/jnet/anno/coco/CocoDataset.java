package com.jnet.anno.coco;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * COCO数据集根对象
 */
@Data
public class CocoDataset {
    
    /**
     * 标注列表
     */
    private List<CocoAnnotation> annotations;
    
    /**
     * 类别列表
     */
    private List<CocoCategory> categories;
    
    /**
     * 图片列表
     */
    private List<CocoImage> images;
    
    /**
     * 数据集信息
     */
    private Map<String, Object> info;
    
    /**
     * 许可证列表
     */
    private List<Map<String, Object>> licenses;
}

package com.jnet.anno.coco;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * COCO数据集图片实体
 */
@Data
public class CocoImage {
    
    /**
     * 图片ID
     */
    private Long id;
    
    /**
     * 文件名
     */
    @JsonProperty("file_name")
    private String fileName;
    
    /**
     * 图片宽度
     */
    private Integer width;
    
    /**
     * 图片高度
     */
    private Integer height;
    
    /**
     * 许可证
     */
    private String license;
    
    /**
     * Flickr URL
     */
    @JsonProperty("flickr_url")
    private String flickrUrl;
    
    /**
     * COCO URL
     */
    @JsonProperty("coco_url")
    private String cocoUrl;
    
    /**
     * 捕获日期
     */
    @JsonProperty("date_captured")
    private String dateCaptured;
}

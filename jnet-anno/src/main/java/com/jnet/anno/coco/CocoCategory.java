package com.jnet.anno.coco;

import lombok.Data;

/**
 * COCO数据集类别实体
 */
@Data
public class CocoCategory {
    
    /**
     * 类别ID
     */
    private Long id;
    
    /**
     * 类别名称
     */
    private String name;
    
    /**
     * 父类别名称
     */
    private String supercategory;
}

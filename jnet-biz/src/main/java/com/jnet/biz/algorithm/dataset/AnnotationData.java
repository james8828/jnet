package com.jnet.biz.algorithm.dataset;

import lombok.Data;

import java.util.Map;

/**
 * 标注数据
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class AnnotationData {
    private Long annotationId;
    private String type;              // polygon/rectangle/point
    private String className;         // 类别名称
    private Integer classId;          // 类别ID
    private Object coordinates;       // 坐标数据（根据类型不同而不同）
    private Map<String, Object> attributes; // 附加属性
}

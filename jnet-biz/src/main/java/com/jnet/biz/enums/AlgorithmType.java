package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 算法类型枚举
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Getter
@AllArgsConstructor
public enum AlgorithmType {

    /**
     * YOLO目标检测算法
     */
    YOLO("YOLO", "YOLO目标检测"),

    /**
     * COCO格式数据集
     */
    COCO("COCO", "COCO格式"),

    /**
     * VOC格式数据集
     */
    VOC("VOC", "PASCAL VOC格式"),

    /**
     * Segment Anything Model
     */
    SAM("SAM", "Segment Anything模型"),

    /**
     * 图像分类算法
     */
    CLASSIFICATION("CLASSIFICATION", "图像分类");



    /**
     * 算法类型代码
     */
    private final String code;

    /**
     * 算法描述
     */
    private final String description;



    /**
     * 根据code获取枚举
     */
    public static AlgorithmType fromCode(String code) {
        for (AlgorithmType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的算法类型: " + code);
    }

    /**
     * 判断是否为有效的算法类型
     */
    public static boolean isValid(String code) {
        for (AlgorithmType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }
}

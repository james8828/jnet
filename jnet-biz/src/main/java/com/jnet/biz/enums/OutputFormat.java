package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据集输出格式枚举
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Getter
@AllArgsConstructor
public enum OutputFormat {

    /**
     * YOLO v5格式
     */
    YOLOV5("yolov5", "YOLO v5格式"),

    /**
     * YOLO v8格式
     */
    YOLOV8("yolov8", "YOLO v8格式"),

    /**
     * COCO格式
     */
    COCO("coco", "COCO JSON格式"),

    /**
     * PASCAL VOC格式
     */
    VOC("voc", "PASCAL VOC XML格式");

    /**
     * 格式代码
     */
    private final String code;

    /**
     * 格式描述
     */
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static OutputFormat fromCode(String code) {
        for (OutputFormat format : values()) {
            if (format.getCode().equalsIgnoreCase(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("未知的输出格式: " + code);
    }

    /**
     * 判断是否为有效的输出格式
     */
    public static boolean isValid(String code) {
        for (OutputFormat format : values()) {
            if (format.getCode().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }
}

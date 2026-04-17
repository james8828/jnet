package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标注类型枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum AnnotationType {

    /**
     * 点标注
     */
    POINT("POINT", "点标注"),

    /**
     * 线标注
     */
    LINESTRING("LINESTRING", "线标注"),

    /**
     * 多边形标注
     */
    POLYGON("POLYGON", "多边形标注"),

    /**
     * 多个多边形
     */
    MULTIPOLYGON("MULTIPOLYGON", "多个多边形");

    /**
     * 类型代码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static AnnotationType fromCode(String code) {
        for (AnnotationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的标注类型: " + code);
    }
}

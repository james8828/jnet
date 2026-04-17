package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标注创建来源枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum CreationSource {

    /**
     * AI预标注
     */
    AI_PRE_ANNOTATION("AI_PRE_ANNOTATION", "AI预标注"),

    /**
     * 人工绘制
     */
    MANUAL_DRAWING("MANUAL_DRAWING", "人工绘制"),

    /**
     * 自动分割
     */
    AUTO_SEGMENTATION("AUTO_SEGMENTATION", "自动分割");

    /**
     * 来源代码
     */
    private final String code;

    /**
     * 来源描述
     */
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static CreationSource fromCode(String code) {
        for (CreationSource source : values()) {
            if (source.getCode().equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("未知的创建来源: " + code);
    }
}

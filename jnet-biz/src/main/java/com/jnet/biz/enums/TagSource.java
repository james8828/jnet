package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标签来源枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum TagSource {

    /**
     * AI预标注
     */
    AI_PRE_ANNOTATION("AI_PRE_ANNOTATION", "AI预标注"),

    /**
     * 人工标注
     */
    MANUAL("MANUAL", "人工标注"),

    /**
     * 系统自动
     */
    SYSTEM_AUTO("SYSTEM_AUTO", "系统自动");

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
    public static TagSource fromCode(String code) {
        for (TagSource source : values()) {
            if (source.getCode().equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("未知的标签来源: " + code);
    }
}

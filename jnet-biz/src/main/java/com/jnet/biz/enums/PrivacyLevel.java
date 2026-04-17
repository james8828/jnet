package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 隐私级别枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum PrivacyLevel {

    /**
     * 公开
     */
    PUBLIC(1, "公开"),

    /**
     * 脱敏
     */
    DESENSITIZED(2, "脱敏"),

    /**
     * 绝密
     */
    TOP_SECRET(3, "绝密");

    /**
     * 级别代码
     */
    private final Integer code;

    /**
     * 级别描述
     */
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static PrivacyLevel fromCode(Integer code) {
        for (PrivacyLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知的隐私级别: " + code);
    }
}

package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目状态枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum ProjectStatus {

    /**
     * 活跃
     */
    ACTIVE("active", "活跃"),

    /**
     * 已归档
     */
    ARCHIVED("archived", "已归档"),

    /**
     * 已删除
     */
    DELETED("deleted", "已删除");

    /**
     * 状态码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static ProjectStatus fromCode(String code) {
        for (ProjectStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的项目状态: " + code);
    }
}

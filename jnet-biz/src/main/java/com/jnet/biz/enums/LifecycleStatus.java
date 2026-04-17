package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图像生命周期状态枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum LifecycleStatus {

    /**
     * 原始数据（刚上传）
     */
    RAW("Raw", "原始数据"),

    /**
     * 已索引（元数据提取完成）
     */
    INDEXED("Indexed", "已索引"),

    /**
     * 处理中（正在进行预处理）
     */
    PROCESSING("Processing", "处理中"),

    /**
     * 已标注（完成人工标注）
     */
    ANNOTATED("Annotated", "已标注"),

    /**
     * 已验证（标注已通过质检）
     */
    VERIFIED("Verified", "已验证"),

    /**
     * 已预测（AI推理完成）
     */
    PREDICTED("Predicted", "已预测"),

    /**
     * 已归档
     */
    ARCHIVED("Archived", "已归档");

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
    public static LifecycleStatus fromCode(String code) {
        for (LifecycleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的生命周期状态: " + code);
    }
}

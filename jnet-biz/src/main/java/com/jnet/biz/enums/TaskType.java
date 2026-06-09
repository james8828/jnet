package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务类型枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum TaskType {

    /**
     * 数据集构建
     */
    DATASET_BUILD("DATASET_BUILD", "数据集构建"),

    /**
     * 模型训练
     */
    TRAINING("TRAINING", "模型训练"),

    /**
     * AI预测/推理
     */
    PREDICTION("PREDICTION", "AI预测"),

    /**
     * 预标注
     */
    PRE_ANNOTATION("PRE_ANNOTATION", "预标注"),

    /**
     * 模型评估
     */
    EVALUATION("EVALUATION", "模型评估");

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
    public static TaskType fromCode(String code) {
        for (TaskType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的任务类型: " + code);
    }

    /**
     * 判断是否为有效的任务类型
     */
    public static boolean isValid(String code) {
        for (TaskType type : values()) {
            if (type.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}

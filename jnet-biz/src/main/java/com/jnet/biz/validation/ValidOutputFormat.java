package com.jnet.biz.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 输出格式校验注解
 * 确保输出格式只能是预定义的值
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = OutputFormatValidator.class)
public @interface ValidOutputFormat {
    
    /**
     * 错误消息
     */
    String message() default "不支持的输出格式: ${validatedValue}。支持的格式: yolov5, yolov8, coco, voc";
    
    /**
     * 分组
     */
    Class<?>[] groups() default {};
    
    /**
     * 载荷
     */
    Class<? extends Payload>[] payload() default {};
}

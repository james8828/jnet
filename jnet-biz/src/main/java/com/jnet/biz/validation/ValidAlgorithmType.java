package com.jnet.biz.validation;

import com.jnet.biz.enums.AlgorithmType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 算法类型校验注解
 * 确保算法类型只能是预定义的值
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AlgorithmTypeValidator.class)
public @interface ValidAlgorithmType {
    
    /**
     * 错误消息
     */
    String message() default "不支持的算法类型: ${validatedValue}。支持的类型: YOLO, COCO, VOC, SAM, CLASSIFICATION";
    
    /**
     * 分组
     */
    Class<?>[] groups() default {};
    
    /**
     * 载荷
     */
    Class<? extends Payload>[] payload() default {};
}

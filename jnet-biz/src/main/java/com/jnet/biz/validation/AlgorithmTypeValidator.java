package com.jnet.biz.validation;

import com.jnet.biz.enums.AlgorithmType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 算法类型校验器实现
 *
 * @author JNet Team
 * @since 2024-05-11
 */
public class AlgorithmTypeValidator implements ConstraintValidator<ValidAlgorithmType, String> {
    
    @Override
    public void initialize(ValidAlgorithmType constraintAnnotation) {
        // 初始化逻辑（如果需要）
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 值由 @NotBlank 或 @NotNull 处理，这里只验证非空值
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        
        // 使用枚举的 isValid 方法验证
        return AlgorithmType.isValid(value);
    }
}

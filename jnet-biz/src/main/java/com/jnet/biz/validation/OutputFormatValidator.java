package com.jnet.biz.validation;

import com.jnet.biz.enums.OutputFormat;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 输出格式校验器实现
 *
 * @author JNet Team
 * @since 2024-05-11
 */
public class OutputFormatValidator implements ConstraintValidator<ValidOutputFormat, String> {
    
    @Override
    public void initialize(ValidOutputFormat constraintAnnotation) {
        // 初始化逻辑（如果需要）
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 值由其他注解处理，这里只验证非空值
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        
        // 使用枚举的 isValid 方法验证
        return OutputFormat.isValid(value);
    }
}

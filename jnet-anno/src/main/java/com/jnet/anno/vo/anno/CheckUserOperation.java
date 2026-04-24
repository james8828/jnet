package com.jnet.anno.vo.anno;

import lombok.Data;

import java.util.List;

/**
 * 检查用户操作
 *
 * @author JNet Team
 * @since 2025-06-10
 */
@Data
public class CheckUserOperation {

    private Long userId;
    private List<Long> slideId;
}

package com.jnet.anno.vo.anno;

import lombok.Data;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/6/10 14:22:29
 */
@Data
public class CheckUserOperation {

    private Long userId;
    private List<Long> slideId;
}

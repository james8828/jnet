package com.jnet.anno.utils.annotation;

import lombok.Builder;
import lombok.Data;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/28 16:50:03
 */
@Builder
@Data
public class UndoRedoReq {
    private Long userId;
    private Long slideId;
}

package com.jnet.anno.utils.annotation;

import com.jnet.anno.domain.Annotation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/28 15:03:44
 */
@Builder
@Data
@Slf4j
public class UndoRedoDetail  implements Serializable {
    private static final long serialVersionUID = 1L;

    private Annotation currentAnnotation;

    private Annotation historyAnnotation;

    @Schema(description = "操作：修改-UPDATE,删除-DELETE,添加-INSERT")
    private String operation;

}

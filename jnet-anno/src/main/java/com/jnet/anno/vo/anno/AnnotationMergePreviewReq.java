package com.jnet.anno.vo.anno;

import lombok.Data;

import java.util.List;

/**
 * 标注合并预览请求
 *
 * @author JNet Team
 * @since 2025-06-06
 */
@Data
public class AnnotationMergePreviewReq {
    private List<Long> markingIdList;
    private Long slideId;
}

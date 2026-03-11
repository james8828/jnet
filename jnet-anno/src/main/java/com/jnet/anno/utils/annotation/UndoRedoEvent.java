package com.jnet.anno.utils.annotation;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/28 15:03:44
 */
@Builder
@Data
@Slf4j
public class UndoRedoEvent implements Serializable {

    private static final long serialVersionUID = 1L; // 推荐显式定义 serialVersionUID

    private Long slideId;

    private Long userId;

    private List<UndoRedoDetail> undoRedoDetails;
}

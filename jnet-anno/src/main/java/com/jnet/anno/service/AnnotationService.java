package com.jnet.anno.service;

import com.jnet.anno.domain.Annotation;
import com.jnet.anno.utils.annotation.UndoRedoReq;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.anno.vo.anno.*;
import com.jnet.api.R;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description 标注管理
 * @date 2025/5/21 14:33:43
 */
public interface AnnotationService extends IService<Annotation> {

    Annotation addAnnotation(AnnotationVo req) throws Exception;

    R deleteAnnotation(Long id) throws Exception;

    R updateAnnotation(AnnotationUpdateVo req) throws Exception;

    R padding(AnnotationUpdateVo req) throws Exception;

    R stickup(AnnotationUpdateVo req) throws Exception;

    R<Geometry> mergePreview(List<Long> annotationIds) throws Exception;

    R<AnnotationDistanceVo> getDistance(AnnotationDistanceReq req)throws Exception;

    R<Geometry> annotationOperation(AnnotationOperationReq req) throws Exception;

    R<List<AnnotationBatchRespVo>> batch(AnnotationBatchReq req) throws Exception;

    R undoAnnotation(UndoRedoReq req) throws Exception;

    R redoAnnotation(UndoRedoReq req) throws Exception;

    R clearUndoAndRedoStack(UndoRedoReq req)throws Exception;
    R checkUndoAndRedoStatus(UndoRedoReq req)throws Exception;
}

package com.jnet.anno.service.impl;

import com.jnet.anno.constant.Constant;
import com.jnet.anno.domain.Annotation;
import com.jnet.anno.domain.Measure;
import com.jnet.anno.mapper.AnnotationMapper;
import com.jnet.anno.mapper.MeasureMapper;
import com.jnet.anno.netty.websocket.NioWebSocketHandler;
import com.jnet.anno.service.AnnotationService;
import com.jnet.anno.utils.MessageSource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.anno.utils.annotation.*;
import com.jnet.anno.vo.anno.*;
import com.jnet.api.R;
import com.jnet.common.core.utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mugw
 * @version 1.0
 * @description 标注管理
 * @date 2025/5/21 14:33:43
 */
@Slf4j
@Service
public class AnnotationServiceImpl extends ServiceImpl<AnnotationMapper, Annotation>
        implements AnnotationService {

    @Resource
    private NioWebSocketHandler webSocketHandler;
    @Resource
    private MeasureMapper measureMapper;
    @Resource
    private UndoRedoManager undoRedoManager;

    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final int SAMPLE_POINTS_COUNT = -1; // 每个几何体采样点数

    /**
     * 计算两个 Geometry 的平均距离（基于采样点）
     */
    public static double calculateAverageDistance(Geometry geom1, Geometry geom2) {
        List<Coordinate> samples1 = sampleGeometry(geom1, SAMPLE_POINTS_COUNT);
        List<Coordinate> samples2 = sampleGeometry(geom2, SAMPLE_POINTS_COUNT);

        double totalDistance = 0.0;
        int count = 0;

        for (Coordinate c1 : samples1) {
            for (Coordinate c2 : samples2) {
                totalDistance += c1.distance(c2);
                count++;
            }
        }

        return count > 0 ? totalDistance / count : 0.0;
    }

    /**
     * 在 Geometry 上均匀采样若干点（这里简化为边界等距点）
     */
    public static List<Coordinate> sampleGeometry(Geometry geom, int numPoints) {
        List<Coordinate> result = new ArrayList<>();
        Coordinate[] coords = geom.getCoordinates();

        if (coords.length == 0) return result;

        if (numPoints == -1) {
            return Arrays.asList(coords);
        }
        // 简单线性插值采样
        for (int i = 0; i < numPoints; i++) {
            double index = (double) i / (numPoints - 1) * (coords.length - 1);
            int idx = (int) Math.floor(index);
            int idxNext = Math.min(idx + 1, coords.length - 1);
            double t = index - idx;

            Coordinate c1 = coords[idx];
            Coordinate c2 = coords[idxNext];

            double x = c1.x + t * (c2.x - c1.x);
            double y = c1.y + t * (c2.y - c1.y);
            result.add(new Coordinate(x, y));
        }

        return result;
    }

    /**
     * 获取几何对象
     * @param geometryId
     * @param annotationType
     * @return
     * @throws Exception
     */
    private Geometry getGeometry(Long geometryId, String annotationType) throws Exception {
        Geometry geometry = null;
        if (annotationType.equals(Constant.ANNO_TYPE_DRAW)){
            Annotation annotation = baseMapper.selectById(geometryId);
            if (annotation == null) {
                throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
            }
            geometry = annotation.getGeometry();
        }else if (annotationType.equals(Constant.ANNO_TYPE_MEASURE)){
            Measure measure = measureMapper.selectById(geometryId);
            if (measure == null) {
                throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
            }
            geometry = measure.getGeometry();
        }
        return geometry;
    }

    @Override
    public R<AnnotationDistanceVo> getDistance(AnnotationDistanceReq req) throws Exception {

        String annotationTypeOne = req.getAnnotationTypeOne();
        String annotationTypeTwo = req.getAnnotationTypeTwo();
        Long annotationIdOne = req.getAnnotationIdOne();
        Long annotationIdTwo = req.getAnnotationIdTwo();

        // 获取几何对象
        Geometry geometryOne = getGeometry(annotationIdOne,annotationTypeOne);
        Geometry geometryTwo = getGeometry(annotationIdTwo,annotationTypeTwo);

        // 检查几何对象是否为空
        if (geometryOne == null || geometryTwo == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        // 使用 DistanceOp 获取最短距离和对应的两个坐标点
        DistanceOp distanceOp = new DistanceOp(geometryOne, geometryTwo);
        double distance = distanceOp.distance(); // 最短距离
        Coordinate[] nearestPoints = distanceOp.nearestPoints(); // 最近的两个点

        // 确保获取到了最近点
        if (nearestPoints == null || nearestPoints.length < 2) {
            throw new Exception(MessageSource.M("FAILED_TO_CALCULATE_NEAREST_POINTS"));
        }

        // 计算平均间距
        double meanDistance = calculateAverageDistance(geometryOne, geometryTwo);

        //  创建 Point 对象
        Point pointOne = geometryFactory.createPoint(nearestPoints[0]);
        Point pointTwo = geometryFactory.createPoint(nearestPoints[1]);

        // 构造返回结果
        AnnotationDistanceVo result = new AnnotationDistanceVo();
        result.setMinDistance(distance);
        result.setPointOne(pointOne);
        result.setPointTwo(pointTwo);
        result.setMeanDistance(meanDistance);
        return R.success(result);
    }

    @Override
    public Annotation addAnnotation(AnnotationVo req) throws Exception {
        return addAnnotation(req, false);
    }

    private Annotation addAnnotation(AnnotationVo req, boolean isUndoRedo) throws Exception {


        req.setCreateBy(SecurityUtils.getUserId());
        req.setUpdateBy(SecurityUtils.getUserId());
        baseMapper.insert(req);
        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder().slideId(req.getSlideId()).userId(SecurityUtils.getUserId())
                    .undoRedoDetails(Arrays.asList(UndoRedoDetail.builder().currentAnnotation(req).operation(Constant.ANNO_ACTION_ADD).build())).build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        }
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(req, Constant.ANNO_ACTION_ADD));
        return req;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public R deleteAnnotation(Long id) throws Exception {
        return deleteAnnotation(id, false);
    }

    private R deleteAnnotation(Long id, boolean isUndoRedo) throws Exception {
        Annotation annotation = baseMapper.selectById(id);
        if (annotation == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }
        baseMapper.deleteById(id);
        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder().slideId(annotation.getSlideId()).userId(SecurityUtils.getUserId())
                    .undoRedoDetails(Arrays.asList(UndoRedoDetail.builder().currentAnnotation(annotation).operation(Constant.ANNO_ACTION_DELETE).build())).build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        }
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_DELETE));

        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }

    @Override
    public R updateAnnotation(AnnotationUpdateVo req) throws Exception {
        return updateAnnotation(req, false);
    }

    private R updateAnnotation(AnnotationUpdateVo req, boolean isUndoRedo) throws Exception {
        if (req.getAnnotationId() == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }
        Geometry geometry = req.getGeometry();
        if (geometry != null) {
            try {
                double area = geometry.getArea();
                double length = geometry.getLength();
                req.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
                req.setPerimeter(BigDecimal.valueOf(length * Constant.IMAGE_RESOLUTION));
            } catch (Exception e) {
                log.error("[{}],计算标注面积失败:[{}]", req, e.getMessage());
            }
        }
        req.setUpdateBy(SecurityUtils.getUserId());
        req.setUpdateTime(new Date());
        Annotation history = baseMapper.selectById(req.getAnnotationId());

        Annotation annotation = new Annotation();
        BeanUtils.copyProperties(annotation, req);
        baseMapper.updateById(annotation);
        annotation = baseMapper.selectById(req.getAnnotationId());
        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder().slideId(annotation.getSlideId()).userId(SecurityUtils.getUserId())
                    .undoRedoDetails(Arrays.asList(UndoRedoDetail.builder().currentAnnotation(annotation).historyAnnotation( history).operation(Constant.ANNO_ACTION_UPDATE).build())).build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        }
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_UPDATE));
        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }

    @Override
    public R padding(AnnotationUpdateVo req) throws Exception {
        Annotation annotation = baseMapper.selectById(req.getAnnotationId());
        Annotation history = new Annotation();
        BeanUtils.copyProperties(history, annotation);
        if (annotation == null || annotation.getGeometry() == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }
        Geometry geometry = annotation.getGeometry();
        if (geometry instanceof GeometryCollection) {
            Geometry firstGeometry = geometry.getGeometryN(0);
            annotation.setGeometry(firstGeometry);
            double area = firstGeometry.getArea();
            double length = firstGeometry.getLength();
            annotation.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
            annotation.setPerimeter(BigDecimal.valueOf(length * Constant.IMAGE_RESOLUTION));
        }
        annotation.setUpdateBy(SecurityUtils.getUserId());
        baseMapper.updateById(annotation);
        UndoRedoEvent event = UndoRedoEvent.builder().slideId(annotation.getSlideId()).userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(UndoRedoDetail.builder().currentAnnotation(annotation).historyAnnotation(history).operation(Constant.ANNO_ACTION_UPDATE).build())).build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_UPDATE));

        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }

    @Override
    public R stickup(AnnotationUpdateVo req) throws Exception {
        Annotation annotation = baseMapper.selectById(req.getAnnotationId());
        annotation.setAnnotationId(null);
        baseMapper.insert(annotation);
        UndoRedoEvent event = UndoRedoEvent.builder().slideId(req.getSlideId()).userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(UndoRedoDetail.builder().currentAnnotation(annotation).operation(Constant.ANNO_ACTION_ADD).build())).build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_ADD));
        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }

    @Override
    public R<Geometry> mergePreview(List<Long> annotationIds) throws Exception {
        if (CollectionUtils.isEmpty(annotationIds)) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }
        List<Annotation> annotationList = listByIds(annotationIds);
        Geometry resp = null;
        if (CollectionUtils.isNotEmpty(annotationList)) {
            if (annotationList.size() == 1) {
                resp = annotationList.get(0).getGeometry();
            } else {
                List<Geometry> geometries = annotationList.stream().map(Annotation::getGeometry).collect(Collectors.toList());
                resp = mergeGeometriesIfIntersect(geometries);
            }
        }
        return R.success(resp);
    }

    private Geometry mergeGeometriesIfIntersect(List<Geometry> geometries) throws Exception {
        if (geometries == null || geometries.size() < 2) {
            return null; // 不足两个几何对象无需合并
        }
        Geometry mergedGeometry = geometries.get(0);
        for (int i = 1; i < geometries.size(); i++) {
            Geometry current = geometries.get(i);
            if (mergedGeometry.intersects(current)) {
                mergedGeometry = mergedGeometry.union(current); // 相交则合并
            }else{
                throw new Exception(MessageSource.M("GRAPHICS_MARK_NOT_RULES"));
            }
        }
        return mergedGeometry;
    }

    @Override
    public R<Geometry> annotationOperation(AnnotationOperationReq req) throws Exception {
        Geometry operationGeometry = req.getGeometry();
        Annotation annotation = baseMapper.selectById(req.getAnnotationId());
        Annotation history = new Annotation();
        BeanUtils.copyProperties(history, annotation);
        if (annotation == null || annotation.getGeometry() == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }
        Geometry geometry = annotation.getGeometry();
        if (req.getCheck()) {
            if (!(operationGeometry instanceof Polygon) || !geometry.intersects(operationGeometry)) {
                throw new Exception(MessageSource.M("GRAPHICS_MARK_NOT_RULES"));
            }
        }
        Geometry result = null;
        if (req.getOperation().equals(Constant.ANNO_OPERATION_UNION)) {
            result = geometry.union(operationGeometry);
        } else if (req.getOperation().equals(Constant.ANNO_OPERATION_DIFFERENCE)) {
            result = geometry.difference(operationGeometry);
        }
        annotation.setGeometry(result);
        annotation.setUpdateBy(SecurityUtils.getUserId());
        annotation.setUpdateTime(new Date());
        double area = result.getArea();
        double length = result.getLength();
        annotation.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
        annotation.setPerimeter(BigDecimal.valueOf(length * Constant.IMAGE_RESOLUTION));
        baseMapper.updateById(annotation);
        UndoRedoEvent event = UndoRedoEvent.builder().slideId(annotation.getSlideId()).userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(UndoRedoDetail.builder().currentAnnotation(annotation).historyAnnotation(history).operation(Constant.ANNO_ACTION_UPDATE).build())).build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_UPDATE));
        return R.success(result);
    }

    @Override
    public R<List<AnnotationBatchRespVo>> batch(AnnotationBatchReq req) throws Exception {
        List<AnnotationBatchRespVo> respList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(req.getList())) {
            List<UndoRedoDetail> undoRedoDetails = new ArrayList<>();
            UndoRedoEvent event = UndoRedoEvent.builder().slideId(req.getSlideId()).userId(SecurityUtils.getUserId()).build();
            for (AnnotationBatchVo annotation : req.getList()) {
                AnnotationBatchRespVo resp = AnnotationBatchRespVo.builder()
                        .status(true)
                        .annotationId(String.valueOf(annotation.getAnnotationId()))
                        .frontId(String.valueOf(annotation.getAnnotationId()))
                        .build();
                try {
                    String operation = annotation.getOperation();
                    Annotation history = baseMapper.selectById(annotation.getAnnotationId());
                    if (Constant.ANNO_OPERATION_UPDATE.equals(operation)) {
                        // 更新操作
                        AnnotationUpdateVo updateVo = new AnnotationUpdateVo();
                        updateVo.setAnnotationId(annotation.getAnnotationId());
                        updateVo.setGeometry(annotation.getGeometry());
                        updateAnnotation(updateVo,true);
                        Annotation anno = new Annotation();
                        BeanUtils.copyProperties(anno, annotation);
                        undoRedoDetails.add(UndoRedoDetail.builder().currentAnnotation(anno).historyAnnotation(history).operation(Constant.ANNO_ACTION_UPDATE).build());
                    } else if (Constant.ANNO_OPERATION_DELETE.equals(operation)) {
                        // 删除操作
                        deleteAnnotation(annotation.getAnnotationId(),true);
                        undoRedoDetails.add(UndoRedoDetail.builder().currentAnnotation(history).operation(Constant.ANNO_ACTION_DELETE).build());
                    } else {
                        // 未知操作
                        annotation.setMessage(MessageSource.M("ANNOTATION_UNKNOWN_OPERATION"));
                        annotation.setStatus(false);
                        resp.setStatus(false);
                    }
                } catch (Exception e) {
                    log.error("批量操作标注数据失败，annotation info: [{}], error: ", annotation, e);
                    annotation.setMessage(e.getMessage());
                    annotation.setStatus(false);
                    resp.setMessage(e.getMessage());
                    resp.setStatus(false);
                    continue;
                }
                respList.add(resp);
            }
            event.setUndoRedoDetails(undoRedoDetails);
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        } else {
            return R.fail(MessageSource.M("ARGUMENT_INVALID"));
        }
        return R.success(respList);
    }

    private R undoDetailHandle(UndoRedoEvent event) {
        List<UndoRedoDetail> undoRedoDetails = event.getUndoRedoDetails();
        if (CollectionUtils.isNotEmpty(undoRedoDetails)) {
            for (UndoRedoDetail undoRedoDetail : undoRedoDetails) {
                try {
                    if (undoRedoDetail.getOperation().equals(Constant.ANNO_ACTION_DELETE)) {
                        AnnotationVo saveVo = new AnnotationVo();
                        BeanUtils.copyProperties(saveVo, undoRedoDetail.getCurrentAnnotation());
                        addAnnotation(saveVo, true);
                    } else if (undoRedoDetail.getOperation().equals(Constant.ANNO_ACTION_UPDATE)) {
                        AnnotationUpdateVo updateVo = new AnnotationUpdateVo();
                        BeanUtils.copyProperties(updateVo, undoRedoDetail.getHistoryAnnotation());
                        updateAnnotation(updateVo, true);
                    } else if (undoRedoDetail.getOperation().equals(Constant.ANNO_ACTION_ADD)) {
                        deleteAnnotation(undoRedoDetail.getCurrentAnnotation().getAnnotationId(), true);
                    }
                } catch (Exception e) {
                    log.error("撤销还原操作标注数据失败，annotation info: [{}], error: [{}]", undoRedoDetail.getCurrentAnnotation(), e.getMessage());
                    continue;
                }
            }
        }
        return R.success();
    }

    private R redoDetailHandle(UndoRedoEvent event) {
        List<UndoRedoDetail> undoRedoDetails = event.getUndoRedoDetails();
        if (CollectionUtils.isNotEmpty(undoRedoDetails)) {
            for (UndoRedoDetail undoRedoDetail : undoRedoDetails) {
                try {
                    if (undoRedoDetail.getOperation().equals(Constant.ANNO_ACTION_DELETE)) {
                        deleteAnnotation(undoRedoDetail.getCurrentAnnotation().getAnnotationId(), true);
                    } else if (undoRedoDetail.getOperation().equals(Constant.ANNO_ACTION_UPDATE)) {
                        AnnotationUpdateVo updateVo = new AnnotationUpdateVo();
                        BeanUtils.copyProperties(updateVo, undoRedoDetail.getCurrentAnnotation());
                        updateAnnotation(updateVo, true);
                    } else if (undoRedoDetail.getOperation().equals(Constant.ANNO_ACTION_ADD)) {
                        AnnotationVo saveVo = new AnnotationVo();
                        BeanUtils.copyProperties(saveVo, undoRedoDetail.getCurrentAnnotation());
                        addAnnotation(saveVo, true);
                    }
                } catch (Exception e) {
                    log.error("撤销还原操作标注数据失败，annotation info: [{}], error: [{}]", undoRedoDetail.getCurrentAnnotation(), e.getMessage());
                    continue;
                }
            }
        }
        return R.success();
    }

    @Override
    public R undoAnnotation(UndoRedoReq req) throws Exception {
        if (!undoRedoManager.canUndo(req.getUserId(), req.getSlideId())) {
            return R.fail(MessageSource.M("ANNOTATION_CANNOT_UNDO"));
        }

        UndoRedoEvent event = undoRedoManager.undo(req.getUserId(), req.getSlideId());
        if (event == null) {
            return R.fail(MessageSource.M("ANNOTATION_NO_HISTORY"));
        }
        return undoDetailHandle(event);
    }

    @Override
    public R redoAnnotation(UndoRedoReq req) throws Exception {
        if (!undoRedoManager.canRedo(req.getUserId(), req.getSlideId())) {
            return R.fail(MessageSource.M("ANNOTATION_CANNOT_REDO"));
        }

        UndoRedoEvent event = undoRedoManager.redo(req.getUserId(), req.getSlideId());
        if (event == null) {
            return R.fail(MessageSource.M("ANNOTATION_NO_FUTURE_STATE"));
        }
        return redoDetailHandle(event);
    }

    @Override
    public R clearUndoAndRedoStack(UndoRedoReq req) throws Exception {
        undoRedoManager.clearForUserAndSlide(req.getUserId(), req.getSlideId());
        return R.success();
    }

    @Override
    public R checkUndoAndRedoStatus(UndoRedoReq req) throws Exception {
        boolean canUndo = undoRedoManager.canUndo(req.getUserId(), req.getSlideId());
        boolean canRedo = undoRedoManager.canRedo(req.getUserId(), req.getSlideId());
        Map<String, Boolean> result = new HashMap<>();
        result.put("undo", canUndo);
        result.put("redo", canRedo);
        return R.success(result);
    }

}





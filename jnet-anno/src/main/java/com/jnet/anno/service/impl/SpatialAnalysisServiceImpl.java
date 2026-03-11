package com.jnet.anno.service.impl;

import cn.hutool.core.date.DateUtil;
import com.jnet.anno.constant.Constant;
import com.jnet.anno.domain.Annotation;
import com.jnet.anno.domain.Measure;
import com.jnet.anno.domain.SpatialAnnotation;
import com.jnet.anno.domain.SpatialMeasure;
import com.jnet.anno.netty.websocket.NioWebSocketHandler;
import com.jnet.anno.repository.SpatialAnnotationRepository;
import com.jnet.anno.repository.SpatialMeasureRepository;
import com.jnet.anno.service.SpatialAnalysisService;
import com.jnet.anno.utils.MessageSource;
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
 * 基于 Hibernate Spatial 的空间分析服务
 * @author mu
 * @version 1.0
 * @since 2026/3/10
 */
@Slf4j
@Service
public class SpatialAnalysisServiceImpl implements SpatialAnalysisService {

    @Resource
    private SpatialAnnotationRepository repository;

    @Resource
    private NioWebSocketHandler webSocketHandler;

    @Resource
    private SpatialMeasureRepository measureRepository;

    @Resource
    private UndoRedoManager undoRedoManager;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final int SAMPLE_POINTS_COUNT = -1;

    public R<AnnotationDistanceVo> getDistance(AnnotationDistanceReq req) throws Exception {
       String annotationTypeOne = req.getAnnotationTypeOne();
       String annotationTypeTwo = req.getAnnotationTypeTwo();
        Long annotationIdOne = req.getAnnotationIdOne();
        Long annotationIdTwo = req.getAnnotationIdTwo();

        // 获取几何对象
        Geometry geometryOne = getGeometry(annotationIdOne, annotationTypeOne);
        Geometry geometryTwo = getGeometry(annotationIdTwo, annotationTypeTwo);

        if (geometryOne == null || geometryTwo == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        // 使用 DistanceOp 获取最短距离和对应的两个坐标点
        DistanceOp distanceOp = new DistanceOp(geometryOne, geometryTwo);
        double distance = distanceOp.distance();
       Coordinate[] nearestPoints = distanceOp.nearestPoints();

        if (nearestPoints == null || nearestPoints.length < 2) {
            throw new Exception(MessageSource.M("FAILED_TO_CALCULATE_NEAREST_POINTS"));
        }

        // 计算平均间距
        double meanDistance = calculateAverageDistance(geometryOne, geometryTwo);

        Point pointOne = GEOMETRY_FACTORY.createPoint(nearestPoints[0]);
        Point pointTwo = GEOMETRY_FACTORY.createPoint(nearestPoints[1]);

        AnnotationDistanceVo result = new AnnotationDistanceVo();
        result.setMinDistance(distance);
        result.setPointOne(pointOne);
        result.setPointTwo(pointTwo);
        result.setMeanDistance(meanDistance);
        return R.success(result);
    }


    @Transactional(rollbackFor = Exception.class)
    public Annotation addAnnotation(AnnotationVo req) throws Exception {
        return addAnnotation(req, false);
    }

    private Annotation addAnnotation(AnnotationVo req, boolean isUndoRedo) throws Exception {
        req.setCreateBy(SecurityUtils.getUserId());
        req.setUpdateBy(SecurityUtils.getUserId());

        // 使用 Hibernate 保存
        SpatialAnnotation spatialAnnotation = new SpatialAnnotation();
        BeanUtils.copyProperties(spatialAnnotation, req);

        if (req.getGeometry() != null) {
            req.getGeometry().setSRID(4326);
            double area = calculateArea(req.getGeometry());
            double length = calculatePerimeter(req.getGeometry());
            req.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
            req.setPerimeter(BigDecimal.valueOf(length * Constant.IMAGE_RESOLUTION));
        }

        spatialAnnotation = repository.save(spatialAnnotation);

        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder()
                    .slideId(req.getSlideId())
                    .userId(SecurityUtils.getUserId())
                    .undoRedoDetails(Arrays.asList(
                            UndoRedoDetail.builder()
                                    .currentAnnotation(convertToAnnotation(spatialAnnotation))
                                    .operation(Constant.ANNO_ACTION_ADD)
                                    .build()))
                    .build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        }

        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(
                convertToAnnotation(spatialAnnotation), Constant.ANNO_ACTION_ADD));

        return convertToAnnotation(spatialAnnotation);
    }


    @Transactional(rollbackFor = Exception.class)
    public R deleteAnnotation(Long id) throws Exception {
        return deleteAnnotation(id, false);
    }

    private R deleteAnnotation(Long id, boolean isUndoRedo) throws Exception {
        SpatialAnnotation annotation= repository.findById(id)
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        repository.delete(annotation);
        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder()
                    .slideId(annotation.getSlideId())
                    .userId(SecurityUtils.getUserId())
                    .undoRedoDetails(Arrays.asList(
                            UndoRedoDetail.builder()
                                    .currentAnnotation(convertToAnnotation(annotation))
                                    .operation(Constant.ANNO_ACTION_DELETE)
                                    .build()))
                    .build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        }

        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(
                convertToAnnotation(annotation), Constant.ANNO_ACTION_DELETE));

        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }


    @Transactional(rollbackFor = Exception.class)
    public R updateAnnotation(AnnotationUpdateVo req) throws Exception {
        return updateAnnotation(req, false);
    }

    private R updateAnnotation(AnnotationUpdateVo req, boolean isUndoRedo) throws Exception {
        if (req.getAnnotationId() == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        SpatialAnnotation annotation = repository.findById(req.getAnnotationId())
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        SpatialAnnotation history = new SpatialAnnotation();
        BeanUtils.copyProperties(history, annotation);

        Geometry geometry = req.getGeometry();
        if (geometry != null) {
            try {
                geometry.setSRID(4326);
                double area = geometry.getArea();
                double length = geometry.getLength();
                req.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
                req.setPerimeter(BigDecimal.valueOf(length * Constant.IMAGE_RESOLUTION));

                annotation.setContour(geometry);
                annotation.setArea(req.getArea());
                annotation.setPerimeter(req.getPerimeter());
            } catch (Exception e) {
                log.error("[{}], 计算标注面积失败:[{}]", req, e.getMessage());
            }
        }

        annotation.setUpdateBy(SecurityUtils.getUserId());
        annotation.setUpdateTime(DateUtil.date());

        if (req.getDescription() != null) {
            annotation.setDescription(req.getDescription());
        }
        if (req.getTagId() != null) {
            annotation.setTagId(req.getTagId());
        }

        annotation = repository.save(annotation);

        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder()
                    .slideId(annotation.getSlideId())
                    .userId(SecurityUtils.getUserId())
                    .undoRedoDetails(Arrays.asList(
                            UndoRedoDetail.builder()
                                    .currentAnnotation(convertToAnnotation(annotation))
                                    .historyAnnotation(convertToAnnotation(history))
                                    .operation(Constant.ANNO_ACTION_UPDATE)
                                    .build()))
                    .build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        }

        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(
                convertToAnnotation(annotation), Constant.ANNO_ACTION_UPDATE));

        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }


    @Transactional(rollbackFor = Exception.class)
    public R padding(AnnotationUpdateVo req) throws Exception {
        SpatialAnnotation annotation = repository.findById(req.getAnnotationId())
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        if (annotation.getContour() == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        SpatialAnnotation history = new SpatialAnnotation();
        BeanUtils.copyProperties(history, annotation);

        Geometry geometry = annotation.getContour();
        if (geometry instanceof Polygon polygon) {
            LinearRing exteriorRing = polygon.getExteriorRing();
            polygon= GEOMETRY_FACTORY.createPolygon(exteriorRing, new LinearRing[0]);
            annotation.setContour(polygon);

            double area = polygon.getArea();
            double length = polygon.getLength();
            annotation.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
            annotation.setPerimeter(BigDecimal.valueOf(length * Constant.IMAGE_RESOLUTION));
        }

        annotation.setUpdateBy(SecurityUtils.getUserId());
        annotation.setUpdateTime(DateUtil.date());
        repository.save(annotation);

        UndoRedoEvent event = UndoRedoEvent.builder()
                .slideId(annotation.getSlideId())
                .userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(
                        UndoRedoDetail.builder()
                                .currentAnnotation(convertToAnnotation(annotation))
                                .historyAnnotation(convertToAnnotation(history))
                                .operation(Constant.ANNO_ACTION_UPDATE)
                                .build()))
                .build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);

        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(
                convertToAnnotation(annotation), Constant.ANNO_ACTION_UPDATE));

        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }


    @Transactional(rollbackFor = Exception.class)
    public R stickup(AnnotationUpdateVo req) throws Exception {
        SpatialAnnotation annotation= repository.findById(req.getAnnotationId())
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        annotation.setAnnotationId(null);
        repository.save(annotation);

        UndoRedoEvent event = UndoRedoEvent.builder()
                .slideId(req.getSlideId())
                .userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(
                        UndoRedoDetail.builder()
                                .currentAnnotation(convertToAnnotation(annotation))
                                .operation(Constant.ANNO_ACTION_ADD)
                                .build()))
                .build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);

        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(
                convertToAnnotation(annotation), Constant.ANNO_ACTION_ADD));

        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }


    @Transactional(readOnly = true)
    public R<Geometry> mergePreview(List<Long> annotationIds) throws Exception {
        if (CollectionUtils.isEmpty(annotationIds)) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        List<SpatialAnnotation> annotationList = repository.findByIds(annotationIds);
        Geometry resp = null;

        if (CollectionUtils.isNotEmpty(annotationList)) {
            if (annotationList.size() == 1) {
                resp = annotationList.get(0).getContour();
            } else {
                List<Geometry> geometries = annotationList.stream()
                        .map(SpatialAnnotation::getContour)
                        .collect(Collectors.toList());
                resp = mergeGeometriesIfIntersect(geometries);
            }
        }

        return R.success(resp);
    }

    private Geometry mergeGeometriesIfIntersect(List<Geometry> geometries) throws Exception {
        if (geometries == null || geometries.size() < 2) {
            return null;
        }

        Geometry mergedGeometry = geometries.get(0);
        for (int i = 1; i < geometries.size(); i++) {
            Geometry current = geometries.get(i);
            if (mergedGeometry.intersects(current)) {
                mergedGeometry = mergedGeometry.union(current);
            } else {
                throw new Exception(MessageSource.M("GRAPHICS_MARK_NOT_RULES"));
            }
        }
        return mergedGeometry;
    }


    @Transactional(rollbackFor = Exception.class)
    public R<Geometry> annotationOperation(AnnotationOperationReq req) throws Exception {
        Geometry operationGeometry = req.getGeometry();
        SpatialAnnotation annotation = repository.findById(req.getAnnotationId())
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        if (annotation.getContour() == null) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        SpatialAnnotation history = new SpatialAnnotation();
        BeanUtils.copyProperties(history, annotation);

        Geometry geometry = annotation.getContour();

        if (req.getCheck()) {
            if (!(operationGeometry instanceof Polygon) || !geometry.intersects(operationGeometry)) {
                throw new Exception(MessageSource.M("GRAPHICS_MARK_NOT_RULES"));
            }
        }

        Geometry result = null;
        if (Constant.ANNO_OPERATION_UNION.equals(req.getOperation())) {
            result = geometry.union(operationGeometry);
        } else if (Constant.ANNO_OPERATION_DIFFERENCE.equals(req.getOperation())) {
            result = geometry.difference(operationGeometry);
        }

        annotation.setContour(result);
        annotation.setUpdateBy(SecurityUtils.getUserId());
        annotation.setUpdateTime(DateUtil.date());

        double area = result.getArea();
        double length = result.getLength();
        annotation.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
        annotation.setPerimeter(BigDecimal.valueOf(length * Constant.IMAGE_RESOLUTION));

        repository.save(annotation);

        UndoRedoEvent event = UndoRedoEvent.builder()
                .slideId(annotation.getSlideId())
                .userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(
                        UndoRedoDetail.builder()
                                .currentAnnotation(convertToAnnotation(annotation))
                                .historyAnnotation(convertToAnnotation(history))
                                .operation(Constant.ANNO_ACTION_UPDATE)
                                .build()))
                .build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);

        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(
                convertToAnnotation(annotation), Constant.ANNO_ACTION_UPDATE));

        return R.success(result);
    }


    @Transactional(rollbackFor = Exception.class)
    public R<List<AnnotationBatchRespVo>> batch(AnnotationBatchReq req) throws Exception {
        List<AnnotationBatchRespVo> respList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(req.getList())) {
            List<UndoRedoDetail> undoRedoDetails = new ArrayList<>();
            UndoRedoEvent event = UndoRedoEvent.builder()
                    .slideId(req.getSlideId())
                    .userId(SecurityUtils.getUserId())
                    .build();

            for (AnnotationBatchVo annotation : req.getList()) {
                AnnotationBatchRespVo resp = AnnotationBatchRespVo.builder()
                        .status(true)
                        .annotationId(String.valueOf(annotation.getAnnotationId()))
                        .frontId(String.valueOf(annotation.getAnnotationId()))
                        .build();

                try {
                   String operation = annotation.getOperation();
                    SpatialAnnotation history = repository.findById(annotation.getAnnotationId())
                            .orElse(null);

                    if (Constant.ANNO_OPERATION_UPDATE.equals(operation)) {
                        AnnotationUpdateVo updateVo = new AnnotationUpdateVo();
                        updateVo.setAnnotationId(annotation.getAnnotationId());
                        updateVo.setGeometry(annotation.getGeometry());
                        updateAnnotation(updateVo, true);

                        Annotation anno = new Annotation();
                        BeanUtils.copyProperties(anno, annotation);
                        undoRedoDetails.add(UndoRedoDetail.builder()
                                .currentAnnotation(anno)
                                .historyAnnotation(convertToAnnotation(history))
                                .operation(Constant.ANNO_ACTION_UPDATE)
                                .build());

                    } else if (Constant.ANNO_OPERATION_DELETE.equals(operation)) {
                        deleteAnnotation(annotation.getAnnotationId(), true);
                        undoRedoDetails.add(UndoRedoDetail.builder()
                                .currentAnnotation(convertToAnnotation(history))
                                .operation(Constant.ANNO_ACTION_DELETE)
                                .build());
                    } else {
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


    @Transactional(rollbackFor = Exception.class)
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


    @Transactional(rollbackFor = Exception.class)
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


    public R clearUndoAndRedoStack(UndoRedoReq req) throws Exception {
        undoRedoManager.clearForUserAndSlide(req.getUserId(), req.getSlideId());
        return R.success();
    }


    public R checkUndoAndRedoStatus(UndoRedoReq req) throws Exception {
        boolean canUndo = undoRedoManager.canUndo(req.getUserId(), req.getSlideId());
        boolean canRedo = undoRedoManager.canRedo(req.getUserId(), req.getSlideId());
        Map<String, Boolean> result = new HashMap<>();
        result.put("undo", canUndo);
        result.put("redo", canRedo);
        return R.success(result);
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算两个 Geometry 的平均距离（基于采样点）
     */
    public double calculateAverageDistance(Geometry geom1, Geometry geom2) {
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
     * 在 Geometry 上均匀采样若干点
     */
    public List<Coordinate> sampleGeometry(Geometry geom, int numPoints) {
        List<Coordinate> result = new ArrayList<>();
       Coordinate[] coords = geom.getCoordinates();

        if (coords.length == 0) {
            return result;
        }

        if (numPoints == -1) {
            return Arrays.asList(coords);
        }

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
     */
    private Geometry getGeometry(Long geometryId, String annotationType) throws Exception {
        if (Constant.ANNO_TYPE_DRAW.equals(annotationType)) {
            SpatialAnnotation annotation = repository.findByAnnotationId(geometryId)
                    .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));
            return annotation.getContour();
        } else if (Constant.ANNO_TYPE_MEASURE.equals(annotationType)) {
            SpatialMeasure spatialMeasure = measureRepository.findByMeasureId(geometryId);
            if (spatialMeasure == null) {
                return null;
            }
            Measure measure = new Measure();
            org.springframework.beans.BeanUtils.copyProperties(spatialMeasure, measure);

            if (measure == null) {
                throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
            }
            return measure.getGeometry();
        }
        return null;
    }

    /**
     * 计算几何面积
     */
    @Transactional(readOnly = true)
    public Double calculateArea(Geometry geometry) {
        if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
            return geometry.getArea();
        }
        return 0.0;
    }

    /**
     * 计算几何周长
     */
    @Transactional(readOnly = true)
    public Double calculatePerimeter(Geometry geometry) {
        if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            return polygon.getLength();
        } else if (geometry instanceof MultiPolygon) {
            double total = 0;
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Polygon poly = (Polygon) geometry.getGeometryN(i);
                total += poly.getLength();
            }
            return total;
        }
        return 0.0;
    }

    /**
     * 查找指定区域内的所有标注
     */
    @Transactional(readOnly = true)
    public List<SpatialAnnotation> findAnnotationsInRegion(
            Double minX, Double minY, Double maxX, Double maxY, Long slideId) {
        return repository.findByBoundingBox(minX, minY, maxX, maxY, slideId);
    }

    /**
     * 检测标注是否相交
     */
    @Transactional(readOnly = true)
    public boolean checkIntersection(Long annotationId1, Long annotationId2) {
        SpatialAnnotation anno1 = repository.findById(annotationId1).orElse(null);
        SpatialAnnotation anno2 = repository.findById(annotationId2).orElse(null);

        if (anno1 == null || anno2 == null) {
            return false;
        }

        return anno1.getContour().intersects(anno2.getContour());
    }

    /**
     * 合并相交的标注
     */
    @Transactional(rollbackFor = Exception.class)
    public SpatialAnnotation mergeAnnotations(Long annotationId1, Long annotationId2) {
        SpatialAnnotation anno1 = repository.findById(annotationId1)
                .orElseThrow(() -> new IllegalArgumentException("Annotation not found"));
        SpatialAnnotation anno2 = repository.findById(annotationId2)
                .orElseThrow(() -> new IllegalArgumentException("Annotation not found"));

        if (!anno1.getContour().intersects(anno2.getContour())) {
            throw new IllegalArgumentException("Annotations do not intersect");
        }

        Geometry merged = anno1.getContour().union(anno2.getContour());

        anno1.setContour(merged);
        anno1.setArea(BigDecimal.valueOf(calculateArea(merged)));
        anno1.setPerimeter(BigDecimal.valueOf(calculatePerimeter(merged)));

        repository.deleteById(annotationId2);

        return repository.save(anno1);
    }

    /**
     * 简化几何（减少点数）
     */
    @Transactional(rollbackFor = Exception.class)
    public SpatialAnnotation simplifyAnnotation(Long annotationId, double tolerance) {
        SpatialAnnotation annotation = repository.findById(annotationId)
                .orElseThrow(() -> new IllegalArgumentException("Annotation not found"));

        Geometry simplified = annotation.getContour();
        annotation.setContour(simplified);
        return repository.save(annotation);
    }

    /**
     * 撤销处理
     */
    private R undoDetailHandle(UndoRedoEvent event) {
        List<UndoRedoDetail> undoRedoDetails = event.getUndoRedoDetails();
        if (CollectionUtils.isNotEmpty(undoRedoDetails)) {
            for (UndoRedoDetail undoRedoDetail : undoRedoDetails) {
                try {
                    if (Constant.ANNO_ACTION_DELETE.equals(undoRedoDetail.getOperation())) {
                        AnnotationVo saveVo = new AnnotationVo();
                        BeanUtils.copyProperties(saveVo, undoRedoDetail.getCurrentAnnotation());
                        addAnnotation(saveVo, true);
                    } else if (Constant.ANNO_ACTION_UPDATE.equals(undoRedoDetail.getOperation())) {
                        AnnotationUpdateVo updateVo = new AnnotationUpdateVo();
                        BeanUtils.copyProperties(updateVo, undoRedoDetail.getHistoryAnnotation());
                        updateAnnotation(updateVo, true);
                    } else if (Constant.ANNO_ACTION_ADD.equals(undoRedoDetail.getOperation())) {
                        deleteAnnotation(undoRedoDetail.getCurrentAnnotation().getAnnotationId(), true);
                    }
                } catch (Exception e) {
                    log.error("撤销还原操作标注数据失败，annotation info: [{}], error: [{}]",
                            undoRedoDetail.getCurrentAnnotation(), e.getMessage());
                    continue;
                }
            }
        }
        return R.success();
    }

    /**
     * 重做处理
     */
    private R redoDetailHandle(UndoRedoEvent event) {
        List<UndoRedoDetail> undoRedoDetails = event.getUndoRedoDetails();
        if (CollectionUtils.isNotEmpty(undoRedoDetails)) {
            for (UndoRedoDetail undoRedoDetail : undoRedoDetails) {
                try {
                    if (Constant.ANNO_ACTION_DELETE.equals(undoRedoDetail.getOperation())) {
                        deleteAnnotation(undoRedoDetail.getCurrentAnnotation().getAnnotationId(), true);
                    } else if (Constant.ANNO_ACTION_UPDATE.equals(undoRedoDetail.getOperation())) {
                        AnnotationUpdateVo updateVo = new AnnotationUpdateVo();
                        BeanUtils.copyProperties(updateVo, undoRedoDetail.getCurrentAnnotation());
                        updateAnnotation(updateVo, true);
                    } else if (Constant.ANNO_ACTION_ADD.equals(undoRedoDetail.getOperation())) {
                        AnnotationVo saveVo = new AnnotationVo();
                        BeanUtils.copyProperties(saveVo, undoRedoDetail.getCurrentAnnotation());
                        addAnnotation(saveVo, true);
                    }
                } catch (Exception e) {
                    log.error("撤销还原操作标注数据失败，annotation info: [{}], error: [{}]",
                            undoRedoDetail.getCurrentAnnotation(), e.getMessage());
                    continue;
                }
            }
        }
        return R.success();
    }

    /**
     * 将 SpatialAnnotation 转换为 Annotation（兼容旧接口）
     */
    private Annotation convertToAnnotation(SpatialAnnotation spatial) {
        if (spatial == null) {
            return null;
        }
        Annotation annotation = new Annotation();
        annotation.setAnnotationId(spatial.getAnnotationId());
        annotation.setSlideId(spatial.getSlideId());
        annotation.setGeometry(spatial.getContour());
        annotation.setArea(spatial.getArea());
        annotation.setPerimeter(spatial.getPerimeter());
        annotation.setDescription(spatial.getDescription());
        annotation.setTagId(spatial.getTagId());
        annotation.setLocationType(spatial.getLocationType());
        annotation.setAnnotationType(spatial.getAnnotationType());
        annotation.setCreateBy(spatial.getCreateBy());
        annotation.setUpdateBy(spatial.getUpdateBy());
        annotation.setCreateTime(spatial.getCreateTime());
        annotation.setUpdateTime(spatial.getUpdateTime());
        return annotation;
    }
}

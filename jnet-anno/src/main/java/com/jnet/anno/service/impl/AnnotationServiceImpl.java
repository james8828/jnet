package com.jnet.anno.service.impl;

import cn.hutool.core.date.DateUtil;
import com.jnet.anno.constant.Constant;
import com.jnet.anno.domain.Measure;
import com.jnet.anno.domain.Annotation;
import com.jnet.anno.domain.SpatialMeasure;
import com.jnet.anno.netty.websocket.NioWebSocketHandler;
import com.jnet.anno.repository.AnnotationRepository;
import com.jnet.anno.repository.SpatialMeasureRepository;
import com.jnet.anno.service.AnnotationService;
import com.jnet.anno.utils.MessageSource;
import com.jnet.anno.utils.SecurityUtils;
import com.jnet.anno.utils.annotation.*;
import com.jnet.anno.vo.anno.*;
import com.jnet.common.result.Result;
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
 * 标注服务实现类
 *
 * @author mu
 * @version 1.0
 * @since 2026/3/10
 */
@Slf4j
@Service
public class AnnotationServiceImpl implements AnnotationService {

    @Resource
    private AnnotationRepository annotationRepository;

    @Resource
    private NioWebSocketHandler webSocketHandler;

    @Resource
    private SpatialMeasureRepository measureRepository;

    @Resource
    private UndoRedoManager undoRedoManager;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final int SAMPLE_POINTS_COUNT = -1;

    /**
     * 计算两个标注之间的最短距离和平均距离
     * <p>
     * 使用 JTS DistanceOp 计算两个几何对象之间的最短距离，
     * 并通过采样点计算平均距离。
     * </p>
     *
     * @param req 距离计算请求参数
     * @return 包含最短距离、平均距离和最近点对的结果
     * @throws Exception 当标注不存在或计算失败时抛出异常
     */
    public Result<AnnotationDistanceVo> getDistance(AnnotationDistanceReq req) throws Exception {
        log.info("开始计算标注距离，annotationIdOne: {}, annotationTypeOne: {}, annotationIdTwo: {}, annotationTypeTwo: {}",
                req.getAnnotationIdOne(), req.getAnnotationTypeOne(),
                req.getAnnotationIdTwo(), req.getAnnotationTypeTwo());

        String annotationTypeOne = req.getAnnotationTypeOne();
        String annotationTypeTwo = req.getAnnotationTypeTwo();
        Long annotationIdOne = req.getAnnotationIdOne();
        Long annotationIdTwo = req.getAnnotationIdTwo();

        // 获取两个标注的几何对象
        Geometry geometryOne = getGeometry(annotationIdOne, annotationTypeOne);
        Geometry geometryTwo = getGeometry(annotationIdTwo, annotationTypeTwo);

        if (geometryOne == null || geometryTwo == null) {
            log.warn("标注几何对象为空，annotationIdOne: {}, annotationIdTwo: {}", annotationIdOne, annotationIdTwo);
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        // 使用 DistanceOp 计算最短距离和对应的最近点对
        DistanceOp distanceOp = new DistanceOp(geometryOne, geometryTwo);
        double minDistance = distanceOp.distance();
        Coordinate[] nearestPoints = distanceOp.nearestPoints();

        if (nearestPoints == null || nearestPoints.length < 2) {
            log.error("计算最近点失败，geometryOne: {}, geometryTwo: {}", geometryOne, geometryTwo);
            throw new Exception(MessageSource.M("FAILED_TO_CALCULATE_NEAREST_POINTS"));
        }

        // 计算平均间距（基于采样点）
        double meanDistance = calculateAverageDistance(geometryOne, geometryTwo);

        // 创建最近点对的 Point 对象
        Point pointOne = GEOMETRY_FACTORY.createPoint(nearestPoints[0]);
        Point pointTwo = GEOMETRY_FACTORY.createPoint(nearestPoints[1]);

        // 构建返回结果
        AnnotationDistanceVo result = new AnnotationDistanceVo();
        result.setMinDistance(minDistance);
        result.setPointOne(pointOne);
        result.setPointTwo(pointTwo);
        result.setMeanDistance(meanDistance);

        log.info("距离计算完成，最短距离: {}, 平均距离: {}", minDistance, meanDistance);
        return Result.success(result);
    }


    /**
     * 新增标注（公开方法）
     * <p>
     * 创建新的标注记录，自动计算面积和周长，并记录撤销/重做事件。
     * </p>
     *
     * @param annotationDTO 标注数据传输对象
     * @return 包含保存后标注实体的结果对象
     * @throws Exception 当保存失败时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Annotation> addAnnotation(AnnotationDTO annotationDTO) throws Exception {
        log.info("开始新增标注，slideId: {}, imageId: {}, tagId: {}, geomType: {}",
                annotationDTO.getSlideId(), annotationDTO.getImageId(),
                annotationDTO.getTagId(), annotationDTO.getGeomType());
        Annotation annotation = addAnnotation(annotationDTO, Boolean.FALSE);
        return Result.success(MessageSource.M("ANNOTATION_ADD_SUCCESS"), annotation);
    }

    /**
     * 新增标注（内部方法）
     * <p>
     * 核心逻辑：
     * 1. 设置审计字段（创建人、更新人）
     * 2. 复制 DTO 属性到实体
     * 3. 计算面积和周长（如果存在几何对象）
     * 4. 保存到数据库
     * 5. 记录撤销/重做事件
     * 6. 发送 WebSocket 消息通知前端
     * </p>
     *
     * @param annotationDTO 标注数据传输对象
     * @param isUndoRedo    是否为撤销/重做操作（true 则不记录事件）
     * @return 保存后的标注实体
     * @throws Exception 当保存失败时抛出异常
     */
    private Annotation addAnnotation(AnnotationDTO annotationDTO, boolean isUndoRedo) throws Exception {
        // 设置审计字段
        Long userId = SecurityUtils.getUserId();
        annotationDTO.setCreateBy(userId);
        annotationDTO.setUpdateBy(userId);

        // 创建标注实体并复制属性
        Annotation annotation = new Annotation();
        BeanUtils.copyProperties(annotation, annotationDTO);

        // 如果存在几何对象，计算面积和周长
        if (annotationDTO.getGeom() != null) {
            double area = calculateArea(annotationDTO.getGeom());
            double perimeter = calculatePerimeter(annotationDTO.getGeom());
            
            // 转换为微米单位（像素 * 分辨率）
            annotation.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
            annotation.setPerimeter(BigDecimal.valueOf(perimeter * Constant.IMAGE_RESOLUTION));
            
            log.debug("计算几何属性，area: {} 微米², perimeter: {} 微米", area, perimeter);
        }

        // 保存到数据库
        annotation = annotationRepository.save(annotation);
        log.info("标注保存成功，annotationId: {}", annotation.getAnnotationId());

        // 如果不是撤销/重做操作，记录事件
        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder()
                    .slideId(annotationDTO.getSlideId())
                    .userId(userId)
                    .undoRedoDetails(Collections.singletonList(
                            UndoRedoDetail.builder()
                                    .currentAnnotation(annotation)
                                    .operation(Constant.ANNO_ACTION_ADD)
                                    .build()))
                    .build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
            log.debug("撤销/重做事件已记录，slideId: {}, userId: {}", annotationDTO.getSlideId(), userId);
        }

        // 发送 WebSocket 消息通知其他用户
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_ADD));
        log.debug("WebSocket 消息已发送，action: ADD");

        return annotation;
    }


    /**
     * 删除标注（公开方法）
     * <p>
     * 根据标注 ID 删除标注记录，并记录撤销/重做事件。
     * </p>
     *
     * @param id 标注 ID
     * @return 操作结果
     * @throws Exception 当标注不存在或删除失败时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteAnnotation(Long id) throws Exception {
        log.info("开始删除标注，annotationId: {}", id);
        return deleteAnnotation(id, Boolean.FALSE);
    }

    /**
     * 删除标注（内部方法）
     * <p>
     * 核心逻辑：
     * 1. 查询标注是否存在
     * 2. 从数据库中删除标注记录
     * 3. 记录撤销/重做事件（保存被删除的标注信息用于恢复）
     * 4. 发送 WebSocket 消息通知前端移除该标注
     * </p>
     *
     * @param id         标注 ID
     * @param isUndoRedo 是否为撤销/重做操作（true 则不记录事件）
     * @return 操作结果
     * @throws Exception 当标注不存在或删除失败时抛出异常
     */
    private Result<Void> deleteAnnotation(Long id, boolean isUndoRedo) throws Exception {
        log.debug("执行删除操作，annotationId: {}, isUndoRedo: {}", id, isUndoRedo);

        // 查询标注是否存在
        Annotation annotation = annotationRepository.findById(id)
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        log.info("找到待删除标注，slideId: {}, geomType: {}", annotation.getSlideId(), annotation.getGeomType());

        // 从数据库中删除标注记录
        annotationRepository.delete(annotation);
        log.info("标注已从数据库删除，annotationId: {}", id);

        // 如果不是撤销/重做操作，记录事件以便后续恢复
        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder()
                    .slideId(annotation.getSlideId())
                    .userId(SecurityUtils.getUserId())
                    .undoRedoDetails(Arrays.asList(
                            UndoRedoDetail.builder()
                                    .currentAnnotation(annotation)
                                    .operation(Constant.ANNO_ACTION_DELETE)
                                    .build()))
                    .build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
            log.debug("撤销/重做事件已记录，slideId: {}, operation: DELETE", annotation.getSlideId());
        }

        // 发送 WebSocket 消息通知其他用户删除该标注
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_DELETE));
        log.debug("WebSocket 消息已发送，action: DELETE");

        return Result.success(MessageSource.M("ANNOTATION_DELETE_SUCCESS"), null);
    }


    /**
     * 更新标注（公开方法）
     * <p>
     * 更新标注的属性（几何形状、描述、标签等），自动重新计算面积和周长。
     * </p>
     *
     * @param annotationDTO 标注数据传输对象（包含要更新的字段）
     * @return 操作结果
     * @throws Exception 当标注不存在或更新失败时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateAnnotation(AnnotationDTO annotationDTO) throws Exception {
        log.info("开始更新标注，annotationId: {}", annotationDTO.getAnnotationId());
        return updateAnnotation(annotationDTO, Boolean.FALSE);
    }

    /**
     * 更新标注（内部方法）
     * <p>
     * 核心逻辑：
     * 1. 验证标注 ID 是否存在
     * 2. 查询原始标注并保存历史副本（用于撤销）
     * 3. 如果更新了几何对象，重新计算面积和周长
     * 4. 更新审计字段（更新人、更新时间）
     * 5. 更新可选字段（描述、标签）
     * 6. 保存到数据库
     * 7. 记录撤销/重做事件（保存变更前后的数据）
     * 8. 发送 WebSocket 消息通知前端更新显示
     * </p>
     *
     * @param annotationDTO 标注数据传输对象（包含要更新的字段）
     * @param isUndoRedo    是否为撤销/重做操作（true 则不记录事件）
     * @return 操作结果
     * @throws Exception 当标注不存在或更新失败时抛出异常
     */
    private Result<Void> updateAnnotation(AnnotationDTO annotationDTO, boolean isUndoRedo) throws Exception {
        // 验证标注 ID
        if (annotationDTO.getAnnotationId() == null) {
            log.warn("标注 ID 为空，无法更新");
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        log.debug("执行更新操作，annotationId: {}, isUndoRedo: {}", annotationDTO.getAnnotationId(), isUndoRedo);

        // 查询原始标注并保存历史副本（用于撤销）
        Annotation annotation = annotationRepository.findById(annotationDTO.getAnnotationId())
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        Annotation history = new Annotation();
        BeanUtils.copyProperties(history, annotation);
        log.debug("已保存标注历史副本，用于撤销操作");

        // 如果更新了几何对象，重新计算面积和周长（像素单位）
        if (annotationDTO.getGeom() != null) {
            double area = calculateArea(annotationDTO.getGeom());
            double perimeter = calculatePerimeter(annotationDTO.getGeom());
            
            // 转换为微米单位（像素 * 分辨率）
            annotation.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
            annotation.setPerimeter(BigDecimal.valueOf(perimeter * Constant.IMAGE_RESOLUTION));
            
            log.debug("重新计算几何属性，area: {} 微米², perimeter: {} 微米", area, perimeter);
        }

        // 更新审计字段（更新人、更新时间）
        annotation.setUpdateBy(SecurityUtils.getUserId());
        annotation.setUpdateTime(DateUtil.date());

        // 更新可选字段（仅当传入值不为 null 时更新）
        if (annotationDTO.getDescription() != null) {
            annotation.setDescription(annotationDTO.getDescription());
            log.debug("更新标注描述");
        }
        if (annotationDTO.getTagId() != null) {
            annotation.setTagId(annotationDTO.getTagId());
            log.debug("更新标签 ID: {}", annotationDTO.getTagId());
        }

        // 保存到数据库
        annotation = annotationRepository.save(annotation);
        log.info("标注更新成功，annotationId: {}", annotation.getAnnotationId());

        // 如果不是撤销/重做操作，记录事件（保存变更前后的数据）
        if (!isUndoRedo) {
            UndoRedoEvent event = UndoRedoEvent.builder()
                    .slideId(annotation.getSlideId())
                    .userId(SecurityUtils.getUserId())
                    .undoRedoDetails(Arrays.asList(
                            UndoRedoDetail.builder()
                                    .currentAnnotation(annotation)
                                    .historyAnnotation(history)
                                    .operation(Constant.ANNO_ACTION_UPDATE)
                                    .build()))
                    .build();
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
            log.debug("撤销/重做事件已记录，slideId: {}, operation: UPDATE", annotation.getSlideId());
        }

        // 发送 WebSocket 消息通知其他用户更新该标注的显示
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_UPDATE));
        log.debug("WebSocket 消息已发送，action: UPDATE");

        return Result.success(MessageSource.M("ANNOTATION_UPDATE_SUCCESS"), null);
    }


    /**
     * 填充标注（去除孔洞）
     * <p>
     * 将多边形中的孔洞移除，只保留外边界。常用于病理标注中填充组织内部的空白区域。
     * </p>
     *
     * @param annotationId 标注 ID
     * @return 操作结果
     * @throws Exception 当标注不存在或几何对象为空时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> padding(Long annotationId) throws Exception {
        log.info("开始填充标注，annotationId: {}", annotationId);

        // 查询标注并验证几何对象是否存在
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        if (annotation.getGeom() == null) {
            log.warn("标注几何对象为空，无法填充，annotationId: {}", annotationId);
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        log.debug("执行填充操作，原始几何类型: {}", annotation.getGeom().getGeometryType());

        // 保存历史副本用于撤销
        Annotation history = new Annotation();
        BeanUtils.copyProperties(history, annotation);

        // 仅处理多边形类型，移除所有孔洞（interior rings）
        Geometry geometry = annotation.getGeom();
        if (geometry instanceof Polygon polygon) {
            // 获取外边界环，不保留任何内边界环（孔洞）
            LinearRing exteriorRing = polygon.getExteriorRing();
            polygon = GEOMETRY_FACTORY.createPolygon(exteriorRing, new LinearRing[0]);
            annotation.setGeom(polygon);
            
            // 重新计算面积和周长（填充后面积会增大）
            double area = calculateArea(polygon);
            double perimeter = calculatePerimeter(polygon);
            annotation.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
            annotation.setPerimeter(BigDecimal.valueOf(perimeter * Constant.IMAGE_RESOLUTION));
            
            log.info("填充完成，新面积: {} 微米², 新周长: {} 微米", area, perimeter);
        } else {
            log.warn("几何对象不是多边形类型，无法执行填充操作，type: {}", geometry.getGeometryType());
        }

        // 更新审计字段
        annotation.setUpdateBy(SecurityUtils.getUserId());
        annotation.setUpdateTime(DateUtil.date());
        annotationRepository.save(annotation);
        log.info("标注已保存，annotationId: {}", annotationId);

        // 记录撤销/重做事件
        UndoRedoEvent event = UndoRedoEvent.builder()
                .slideId(annotation.getSlideId())
                .userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(
                        UndoRedoDetail.builder()
                                .currentAnnotation(annotation)
                                .historyAnnotation(history)
                                .operation(Constant.ANNO_ACTION_UPDATE)
                                .build()))
                .build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        log.debug("撤销/重做事件已记录，operation: PADDING");

        // 发送 WebSocket 消息通知前端更新显示
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_UPDATE));
        log.debug("WebSocket 消息已发送，action: UPDATE");

        return Result.success(MessageSource.M("ANNOTATION_PADDING_SUCCESS"), null);
    }


    /**
     * 粘贴标注（复制标注）
     * <p>
     * 基于现有标注创建一个完全相同的副本，用于快速复制标注。
     * 新标注会获得新的 ID，其他属性与原标注相同。
     * </p>
     *
     * @param annotationDTO 包含源标注 ID 的数据传输对象
     * @return 操作结果
     * @throws Exception 当源标注不存在时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> stickup(AnnotationDTO annotationDTO) throws Exception {
        log.info("开始粘贴标注，sourceAnnotationId: {}", annotationDTO.getAnnotationId());

        // 查询源标注
        Annotation annotation = annotationRepository.findById(annotationDTO.getAnnotationId())
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        log.debug("找到源标注，slideId: {}, geomType: {}", annotation.getSlideId(), annotation.getGeomType());

        // 清除 ID 以便创建新记录（JPA 会根据 ID 是否为 null 判断是新增还是更新）
        annotation.setAnnotationId(null);
        annotation = annotationRepository.save(annotation);
        
        log.info("标注副本创建成功，newAnnotationId: {}", annotation.getAnnotationId());

        // 记录撤销/重做事件（添加操作）
        UndoRedoEvent event = UndoRedoEvent.builder()
                .slideId(annotationDTO.getSlideId())
                .userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(
                        UndoRedoDetail.builder()
                                .currentAnnotation(annotation)
                                .operation(Constant.ANNO_ACTION_ADD)
                                .build()))
                .build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        log.debug("撤销/重做事件已记录，operation: ADD (STICKUP)");

        // 发送 WebSocket 消息通知前端显示新标注
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_ADD));
        log.debug("WebSocket 消息已发送，action: ADD");

        return Result.success(MessageSource.M("ANNOTATION_STICKUP_SUCCESS"), null);
    }


    /**
     * 合并预览（多个标注的并集）
     * <p>
     * 计算多个标注几何对象的并集，用于预览合并效果。
     * 要求所有标注必须相互交叉或接触，否则抛出异常。
     * </p>
     *
     * @param annotationIds 待合并的标注 ID 列表
     * @return 包含合并后几何对象的结果
     * @throws Exception 当标注列表为空或图形不相交时抛出异常
     */
    @Transactional(readOnly = true)
    public Result<Geometry> mergePreview(List<Long> annotationIds) throws Exception {
        log.info("开始合并预览，annotationIds: {}", annotationIds);

        // 验证输入参数
        if (CollectionUtils.isEmpty(annotationIds)) {
            log.warn("标注 ID 列表为空，无法合并");
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        // 查询所有标注
        List<Annotation> annotationList = annotationRepository.findByIds(annotationIds);
        Geometry resp = null;

        if (CollectionUtils.isNotEmpty(annotationList)) {
            log.debug("找到 {} 个标注，开始合并", annotationList.size());

            if (annotationList.size() == 1) {
                // 只有一个标注，直接返回其几何对象
                resp = annotationList.get(0).getGeom();
                log.debug("仅一个标注，直接返回");
            } else {
                // 多个标注，逐个合并
                List<Geometry> geometries = annotationList.stream()
                        .map(Annotation::getGeom)
                        .collect(Collectors.toList());
                
                Geometry mergedGeometry = geometries.get(0);
                for (int i = 1; i < geometries.size(); i++) {
                    Geometry current = geometries.get(i);
                    
                    // 检查是否相交，只有相交的图形才能合并
                    if (mergedGeometry.intersects(current)) {
                        mergedGeometry = mergedGeometry.union(current);
                        log.debug("合并第 {} 个几何对象", i + 1);
                    } else {
                        log.warn("图形不相交，无法合并，index: {}", i);
                        throw new Exception(MessageSource.M("GRAPHICS_MARK_NOT_RULES"));
                    }
                }
                resp = mergedGeometry;
                log.info("合并完成，最终几何类型: {}", resp.getGeometryType());
            }
        } else {
            log.warn("未找到任何标注，annotationIds: {}", annotationIds);
        }

        return Result.success(resp);
    }


    /**
     * 标注运算（并集/差集）
     * <p>
     * 对指定标注执行布尔运算操作：
     * - 并集（union）：将操作几何与标注几何合并
     * - 差集（difference）：从标注几何中减去操作几何
     * </p>
     *
     * @param req 运算请求参数（包含标注 ID、操作几何、运算类型、是否校验）
     * @return 包含运算后几何对象的结果
     * @throws Exception 当标注不存在、几何对象为空或校验失败时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Geometry> annotationOperation(AnnotationOperationReq req) throws Exception {
        log.info("开始标注运算，annotationId: {}, operation: {}, check: {}",
                req.getAnnotationId(), req.getOperation(), req.getCheck());

        Geometry operationGeometry = req.getGeometry();
        
        // 查询标注并验证几何对象
        Annotation annotation = annotationRepository.findById(req.getAnnotationId())
                .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));

        if (annotation.getGeom() == null) {
            log.warn("标注几何对象为空，annotationId: {}", req.getAnnotationId());
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }

        log.debug("原始几何类型: {}, 操作几何类型: {}", 
                annotation.getGeom().getGeometryType(), operationGeometry.getGeometryType());

        // 保存历史副本用于撤销
        Annotation history = new Annotation();
        BeanUtils.copyProperties(history, annotation);

        Geometry geometry = annotation.getGeom();

        // 如果需要校验，检查操作几何是否为多边形且与标注几何相交
        if (req.getCheck()) {
            if (!(operationGeometry instanceof Polygon) || !geometry.intersects(operationGeometry)) {
                log.warn("几何校验失败，operationGeometry 类型: {}, 是否相交: {}",
                        operationGeometry.getGeometryType(), geometry.intersects(operationGeometry));
                throw new Exception(MessageSource.M("GRAPHICS_MARK_NOT_RULES"));
            }
            log.debug("几何校验通过");
        }

        // 执行布尔运算
        Geometry result = null;
        if (Constant.ANNO_OPERATION_UNION.equals(req.getOperation())) {
            // 并集运算：合并两个几何对象
            result = geometry.union(operationGeometry);
            log.info("执行并集运算");
        } else if (Constant.ANNO_OPERATION_DIFFERENCE.equals(req.getOperation())) {
            // 差集运算：从标注几何中减去操作几何
            result = geometry.difference(operationGeometry);
            log.info("执行差集运算");
        } else {
            log.warn("未知的运算类型: {}", req.getOperation());
        }

        // 更新标注的几何对象和审计字段
        annotation.setGeom(result);
        annotation.setUpdateBy(SecurityUtils.getUserId());
        annotation.setUpdateTime(DateUtil.date());

        // 重新计算面积和周长（像素单位 → 微米单位）
        double area = result.getArea();
        double length = result.getLength();
        annotation.setArea(BigDecimal.valueOf(area * Constant.IMAGE_RESOLUTION_SQUARE));
        annotation.setPerimeter(BigDecimal.valueOf(length * Constant.IMAGE_RESOLUTION));
        
        log.debug("运算后几何属性，area: {} 微米², perimeter: {} 微米", area, length);

        // 保存到数据库
        annotationRepository.save(annotation);
        log.info("标注运算结果已保存，annotationId: {}", annotation.getAnnotationId());

        // 记录撤销/重做事件
        UndoRedoEvent event = UndoRedoEvent.builder()
                .slideId(annotation.getSlideId())
                .userId(SecurityUtils.getUserId())
                .undoRedoDetails(Arrays.asList(
                        UndoRedoDetail.builder()
                                .currentAnnotation(annotation)
                                .historyAnnotation(history)
                                .operation(Constant.ANNO_ACTION_UPDATE)
                                .build()))
                .build();
        undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
        log.debug("撤销/重做事件已记录，operation: {}", req.getOperation());

        // 发送 WebSocket 消息通知前端更新显示
        webSocketHandler.sendMessage(AnnotationMessageGenerator.generateAnnotationMessage(annotation, Constant.ANNO_ACTION_UPDATE));
        log.debug("WebSocket 消息已发送，action: UPDATE");

        return Result.success(result);
    }


    /**
     * 批量操作标注（更新/删除）
     * <p>
     * 支持在一次请求中执行多个标注的更新或删除操作。
     * 所有操作会记录到一个撤销/重做事件中，方便一次性撤销。
     * </p>
     *
     * @param req 批量操作请求（包含 slideId 和操作列表）
     * @return 包含每个操作结果的列表
     * @throws Exception 当参数无效时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<List<AnnotationBatchVo>> batch(AnnotationBatchDTO req) throws Exception {
        log.info("开始批量操作标注，slideId: {}, 操作数量: {}",
                req.getSlideId(), req.getList() != null ? req.getList().size() : 0);

        List<AnnotationBatchVo> respList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(req.getList())) {
            // 创建撤销/重做事件容器，收集所有操作的详细信息
            List<UndoRedoDetail> undoRedoDetails = new ArrayList<>();
            UndoRedoEvent event = UndoRedoEvent.builder()
                    .slideId(req.getSlideId())
                    .userId(SecurityUtils.getUserId())
                    .build();

            log.debug("开始处理 {} 个标注操作", req.getList().size());

            for (AnnotationOperation annotation : req.getList()) {
                // 为每个操作创建响应对象
                AnnotationBatchVo resp = AnnotationBatchVo.builder()
                        .status(true)
                        .annotationId(String.valueOf(annotation.getAnnotationId()))
                        .frontId(String.valueOf(annotation.getAnnotationId()))
                        .build();

                try {
                    String operation = annotation.getOperation();
                    
                    // 查询标注的历史状态（用于撤销）
                    Annotation history = annotationRepository.findById(annotation.getAnnotationId())
                            .orElse(null);

                    if (Constant.ANNO_OPERATION_UPDATE.equals(operation)) {
                        // 更新操作
                        log.debug("执行批量更新，annotationId: {}", annotation.getAnnotationId());
                        AnnotationDTO annotationDTO = new AnnotationDTO();
                        annotationDTO.setAnnotationId(annotation.getAnnotationId());
                        annotationDTO.setGeom(annotation.getGeom());
                        updateAnnotation(annotationDTO, true); // isUndoRedo=true 不单独记录事件

                        // 构建撤销详情
                        Annotation anno = new Annotation();
                        BeanUtils.copyProperties(anno, annotation);
                        undoRedoDetails.add(UndoRedoDetail.builder()
                                .currentAnnotation(anno)
                                .historyAnnotation(history)
                                .operation(Constant.ANNO_ACTION_UPDATE)
                                .build());

                    } else if (Constant.ANNO_OPERATION_DELETE.equals(operation)) {
                        // 删除操作
                        log.debug("执行批量删除，annotationId: {}", annotation.getAnnotationId());
                        deleteAnnotation(annotation.getAnnotationId(), true); // isUndoRedo=true 不单独记录事件
                        // 构建撤销详情（保存被删除的标注信息）
                        undoRedoDetails.add(UndoRedoDetail.builder()
                                .currentAnnotation(history)
                                .operation(Constant.ANNO_ACTION_DELETE)
                                .build());
                    } else {
                        // 未知操作类型
                        log.warn("未知的批量操作类型: {}, annotationId: {}", operation, annotation.getAnnotationId());
                        resp.setStatus(false);
                    }
                } catch (Exception e) {
                    // 单个操作失败不影响其他操作，记录错误并继续
                    log.error("批量操作标注数据失败，annotation info: [{}], error: ", annotation, e);
                    resp.setMessage(e.getMessage());
                    resp.setStatus(false);
                    continue;
                }
                respList.add(resp);
            }

            // 将所有操作记录到一个撤销/重做事件中
            event.setUndoRedoDetails(undoRedoDetails);
            undoRedoManager.addEvent(event, Constant.UNDO_REDO_STACK_SIZE);
            log.info("批量操作完成，成功: {}, 失败: {}",
                    respList.stream().filter(AnnotationBatchVo::getStatus).count(),
                    respList.stream().filter(r -> !r.getStatus()).count());
        } else {
            log.warn("批量操作列表为空");
            return Result.error(MessageSource.M("ARGUMENT_INVALID"));
        }

        return Result.success(respList);
    }


    /**
     * 撤销标注操作
     * <p>
     * 撤销上一次的标注操作（添加/更新/删除），恢复到之前的状态。
     * </p>
     *
     * @param req 撤销请求（包含 userId 和 slideId）
     * @return 操作结果
     * @throws Exception 当无法撤销时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> undoAnnotation(UndoRedoReq req) throws Exception {
        log.info("开始撤销操作，userId: {}, slideId: {}", req.getUserId(), req.getSlideId());

        // 检查是否可以撤销
        if (!undoRedoManager.canUndo(req.getUserId(), req.getSlideId())) {
            log.warn("无法撤销，没有可撤销的操作，userId: {}, slideId: {}", req.getUserId(), req.getSlideId());
            return Result.error(MessageSource.M("ANNOTATION_CANNOT_UNDO"));
        }

        // 执行撤销并获取事件详情
        UndoRedoEvent event = undoRedoManager.undo(req.getUserId(), req.getSlideId());
        if (event == null) {
            log.warn("撤销事件为空，userId: {}, slideId: {}", req.getUserId(), req.getSlideId());
            return Result.error(MessageSource.M("ANNOTATION_NO_HISTORY"));
        }

        log.debug("获取到撤销事件，操作数量: {}", 
                event.getUndoRedoDetails() != null ? event.getUndoRedoDetails().size() : 0);

        // 处理撤销详情（恢复标注状态）
        undoDetailHandle(event);
        return Result.success(MessageSource.M("ANNOTATION_UNDO_SUCCESS"), null);
    }


    /**
     * 重做标注操作
     * <p>
     * 重做之前撤销的标注操作，恢复到撤销前的状态。
     * </p>
     *
     * @param req 重做请求（包含 userId 和 slideId）
     * @return 操作结果
     * @throws Exception 当无法重做时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> redoAnnotation(UndoRedoReq req) throws Exception {
        log.info("开始重做操作，userId: {}, slideId: {}", req.getUserId(), req.getSlideId());

        // 检查是否可以重做
        if (!undoRedoManager.canRedo(req.getUserId(), req.getSlideId())) {
            log.warn("无法重做，没有可重做的操作，userId: {}, slideId: {}", req.getUserId(), req.getSlideId());
            return Result.error(MessageSource.M("ANNOTATION_CANNOT_REDO"));
        }

        // 执行重做并获取事件详情
        UndoRedoEvent event = undoRedoManager.redo(req.getUserId(), req.getSlideId());
        if (event == null) {
            log.warn("重做事件为空，userId: {}, slideId: {}", req.getUserId(), req.getSlideId());
            return Result.error(MessageSource.M("ANNOTATION_NO_FUTURE_STATE"));
        }

        log.debug("获取到重做事件，操作数量: {}",
                event.getUndoRedoDetails() != null ? event.getUndoRedoDetails().size() : 0);

        // 处理重做详情（应用标注状态）
        redoDetailHandle(event);
        return Result.success(MessageSource.M("ANNOTATION_REDO_SUCCESS"), null);
    }


    /**
     * 清除指定用户的撤销/重做栈
     * <p>
     * 清空指定用户在指定切片上的所有撤销/重做历史记录。
     * 通常在关闭切片或切换用户时调用。
     * </p>
     *
     * @param req 清除请求（包含 userId 和 slideId）
     * @return 操作结果
     * @throws Exception 当清除失败时抛出异常
     */
    public Result<Void> clearUndoAndRedoStack(UndoRedoReq req) throws Exception {
        log.info("清除撤销/重做栈，userId: {}, slideId: {}", req.getUserId(), req.getSlideId());
        undoRedoManager.clearForUserAndSlide(req.getUserId(), req.getSlideId());
        log.debug("撤销/重做栈已清除");
        return Result.success(MessageSource.M("ANNOTATION_CLEAR_STACK_SUCCESS"), null);
    }


    /**
     * 检查撤销/重做状态
     * <p>
     * 查询当前是否可以进行撤销或重做操作。
     * </p>
     *
     * @param req 查询请求（包含 userId 和 slideId）
     * @return 包含 canUndo 和 canRedo 两个布尔值的 Map
     * @throws Exception 当查询失败时抛出异常
     */
    public Result<Map<String, Boolean>> checkUndoAndRedoStatus(UndoRedoReq req) throws Exception {
        log.debug("检查撤销/重做状态，userId: {}, slideId: {}", req.getUserId(), req.getSlideId());
        
        boolean canUndo = undoRedoManager.canUndo(req.getUserId(), req.getSlideId());
        boolean canRedo = undoRedoManager.canRedo(req.getUserId(), req.getSlideId());
        
        Map<String, Boolean> result = new HashMap<>();
        result.put("undo", canUndo);
        result.put("redo", canRedo);
        
        log.debug("撤销/重做状态，canUndo: {}, canRedo: {}", canUndo, canRedo);
        return Result.success(result);
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算两个几何对象的平均距离（基于采样点）
     * <p>
     * 通过在两个几何对象上均匀采样，计算所有采样点对之间的距离平均值。
     * 适用于评估两个标注之间的整体接近程度。
     * </p>
     *
     * @param geom1 第一个几何对象
     * @param geom2 第二个几何对象
     * @return 平均距离（像素单位）
     */
    public double calculateAverageDistance(Geometry geom1, Geometry geom2) {
        log.debug("计算平均距离，geom1 type: {}, geom2 type: {}", 
                geom1.getGeometryType(), geom2.getGeometryType());
        
        List<Coordinate> samples1 = sampleGeometry(geom1, SAMPLE_POINTS_COUNT);
        List<Coordinate> samples2 = sampleGeometry(geom2, SAMPLE_POINTS_COUNT);

        double totalDistance = 0.0;
        int count = 0;

        // 计算所有采样点对之间的距离总和
        for (Coordinate c1 : samples1) {
            for (Coordinate c2 : samples2) {
                totalDistance += c1.distance(c2);
                count++;
            }
        }

        double averageDistance = count > 0 ? totalDistance / count : 0.0;
        log.debug("平均距离计算完成，总距离: {}, 点对数量: {}, 平均距离: {}", 
                totalDistance, count, averageDistance);
        
        return averageDistance;
    }

    /**
     * 在几何对象上均匀采样若干点
     * <p>
     * 如果 numPoints 为 -1，则返回所有坐标点；否则进行插值采样。
     * </p>
     *
     * @param geom     待采样的几何对象
     * @param numPoints 采样点数量（-1 表示返回所有点）
     * @return 采样点坐标列表
     */
    public List<Coordinate> sampleGeometry(Geometry geom, int numPoints) {
        List<Coordinate> result = new ArrayList<>();
        Coordinate[] coords = geom.getCoordinates();

        if (coords.length == 0) {
            log.debug("几何对象没有坐标点，type: {}", geom.getGeometryType());
            return result;
        }

        // 如果指定返回所有点，直接转换数组为列表
        if (numPoints == -1) {
            log.debug("返回所有坐标点，数量: {}", coords.length);
            return Arrays.asList(coords);
        }

        // 均匀插值采样
        log.debug("进行插值采样，目标点数: {}, 原始点数: {}", numPoints, coords.length);
        for (int i = 0; i < numPoints; i++) {
            double index = (double) i / (numPoints - 1) * (coords.length - 1);
            int idx = (int) Math.floor(index);
            int idxNext = Math.min(idx + 1, coords.length - 1);
            double t = index - idx;

            Coordinate c1 = coords[idx];
            Coordinate c2 = coords[idxNext];

            // 线性插值计算中间点坐标
            double x = c1.x + t * (c2.x - c1.x);
            double y = c1.y + t * (c2.y - c1.y);
            result.add(new Coordinate(x, y));
        }

        log.debug("采样完成，采样点数: {}", result.size());
        return result;
    }

    /**
     * 获取几何对象
     * <p>
     * 根据标注类型从不同的数据源中获取几何对象：
     * - 绘图标注（draw）：从 Annotation 表查询
     * - 测量标注（measure）：从 SpatialMeasure 表查询
     * </p>
     *
     * @param geometryId     几何对象 ID
     * @param annotationType 标注类型（draw 或 measure）
     * @return 几何对象，如果不存在则返回 null
     * @throws Exception 当标注不存在时抛出异常
     */
    private Geometry getGeometry(Long geometryId, String annotationType) throws Exception {
        if (Constant.ANNO_TYPE_DRAW.equals(annotationType)) {
            // 从标注表查询绘图几何对象
            Annotation annotation = annotationRepository.findByAnnotationId(geometryId)
                    .orElseThrow(() -> new Exception(MessageSource.M("NO_ANNOTATION_DATA")));
            log.debug("获取绘图标注几何对象，annotationId: {}, geomType: {}",
                    geometryId, annotation.getGeom() != null ? annotation.getGeom().getGeometryType() : "null");
            return annotation.getGeom();
        } else if (Constant.ANNO_TYPE_MEASURE.equals(annotationType)) {
            // 从测量表查询测量几何对象
            SpatialMeasure spatialMeasure = measureRepository.findByMeasureId(geometryId);
            if (spatialMeasure == null) {
                log.debug("测量对象不存在，measureId: {}", geometryId);
                return null;
            }
            Measure measure = new Measure();
            org.springframework.beans.BeanUtils.copyProperties(spatialMeasure, measure);

            if (measure == null) {
                log.warn("测量对象转换为空，measureId: {}", geometryId);
                throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
            }
            log.debug("获取测量标注几何对象，measureId: {}, geomType: {}",
                    geometryId, measure.getGeometry() != null ? measure.getGeometry().getGeometryType() : "null");
            return measure.getGeometry();
        }
        log.warn("未知的标注类型: {}", annotationType);
        return null;
    }

    /**
     * 计算几何面积
     * <p>
     * 仅对多边形（Polygon）和多多边形（MultiPolygon）计算面积，其他类型返回 0。
     * </p>
     *
     * @param geometry 几何对象
     * @return 面积值（像素单位）
     */
    public Double calculateArea(Geometry geometry) {
        if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
            double area = geometry.getArea();
            log.debug("计算面积，geomType: {}, area: {} 像素²", geometry.getGeometryType(), area);
            return area;
        }
        log.debug("几何类型不支持面积计算，返回 0，type: {}", geometry.getGeometryType());
        return 0.0;
    }

    /**
     * 计算几何周长
     * <p>
     * 对于多边形返回外边界长度，对于多多边形返回所有子多边形的周长总和。
     * </p>
     *
     * @param geometry 几何对象
     * @return 周长值（像素单位）
     */
    public Double calculatePerimeter(Geometry geometry) {
        if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            double length = polygon.getLength();
            log.debug("计算多边形周长，length: {} 像素", length);
            return length;
        } else if (geometry instanceof MultiPolygon) {
            // 多多边形：累加所有子多边形的周长
            double total = 0;
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Polygon poly = (Polygon) geometry.getGeometryN(i);
                total += poly.getLength();
            }
            log.debug("计算多多边形周长，子多边形数量: {}, total: {} 像素", 
                    geometry.getNumGeometries(), total);
            return total;
        }
        log.debug("几何类型不支持周长计算，返回 0，type: {}", geometry.getGeometryType());
        return 0.0;
    }

    /**
     * 查找指定区域内的所有标注
     * <p>
     * 使用边界框（Bounding Box）查询指定矩形区域内的所有标注。
     * 常用于视图范围内的标注加载。
     * </p>
     *
     * @param minX    最小 X 坐标
     * @param minY    最小 Y 坐标
     * @param maxX    最大 X 坐标
     * @param maxY    最大 Y 坐标
     * @param slideId 切片 ID
     * @return 区域内的标注列表
     */
    @Transactional(readOnly = true)
    public List<Annotation> findAnnotationsInRegion(Double minX, Double minY, Double maxX, Double maxY, Long slideId) {
        log.debug("查询区域内标注，minX: {}, minY: {}, maxX: {}, maxY: {}, slideId: {}",
                minX, minY, maxX, maxY, slideId);
        List<Annotation> annotations = annotationRepository.findByBoundingBox(minX, minY, maxX, maxY, slideId);
        log.debug("查询到 {} 个标注", annotations != null ? annotations.size() : 0);
        return annotations;
    }

    /**
     * 简化几何（减少点数）
     * <p>
     * 使用 Douglas-Peucker 算法简化几何对象，减少顶点数量以提高渲染性能。
     * 注意：当前实现尚未实际调用简化算法，需要后续完善。
     * </p>
     *
     * @param annotationId 标注 ID
     * @param tolerance    容差值（越大简化程度越高）
     * @return 简化后的标注对象
     * @throws IllegalArgumentException 当标注不存在时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Annotation> simplifyAnnotation(Long annotationId, double tolerance) throws Exception {
        log.info("开始简化标注几何，annotationId: {}, tolerance: {}", annotationId, tolerance);
        
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new IllegalArgumentException("Annotation not found"));

        // TODO: 实际调用 JTS 的 DouglasPeuckerSimplifier 进行简化
        Geometry simplified = annotation.getGeom();
        annotation.setGeom(simplified);
        
        annotation = annotationRepository.save(annotation);
        log.info("标注几何简化完成，annotationId: {}", annotationId);
        
        return Result.success(MessageSource.M("ANNOTATION_SIMPLIFY_SUCCESS"), annotation);
    }

    /**
     * 撤销处理
     * <p>
     * 根据撤销事件中的操作类型，反向执行相应的操作：
     * - DELETE → 重新添加被删除的标注
     * - UPDATE → 恢复到历史版本
     * - ADD → 删除新添加的标注
     * </p>
     *
     * @param event 撤销事件（包含所有需要撤销的操作详情）
     * @return 操作结果
     */
    private Result undoDetailHandle(UndoRedoEvent event) {
        List<UndoRedoDetail> undoRedoDetails = event.getUndoRedoDetails();
        if (CollectionUtils.isNotEmpty(undoRedoDetails)) {
            log.info("开始处理撤销，操作数量: {}", undoRedoDetails.size());
            
            for (UndoRedoDetail undoRedoDetail : undoRedoDetails) {
                try {
                    String operation = undoRedoDetail.getOperation();
                    Long annotationId = undoRedoDetail.getCurrentAnnotation().getAnnotationId();
                    
                    log.debug("执行撤销操作，operation: {}, annotationId: {}", operation, annotationId);
                    
                    if (Constant.ANNO_ACTION_DELETE.equals(operation)) {
                        // 撤销删除：重新添加被删除的标注
                        log.debug("恢复被删除的标注，annotationId: {}", annotationId);
                        AnnotationDTO annotationDTO = new AnnotationDTO();
                        BeanUtils.copyProperties(annotationDTO, undoRedoDetail.getCurrentAnnotation());
                        addAnnotation(annotationDTO, true); // isUndoRedo=true 不重复记录事件
                        
                    } else if (Constant.ANNO_ACTION_UPDATE.equals(operation)) {
                        // 撤销更新：恢复到历史版本
                        log.debug("恢复到历史版本，annotationId: {}", annotationId);
                        AnnotationDTO annotationDTO = new AnnotationDTO();
                        BeanUtils.copyProperties(annotationDTO, undoRedoDetail.getHistoryAnnotation());
                        updateAnnotation(annotationDTO, true); // isUndoRedo=true 不重复记录事件
                        
                    } else if (Constant.ANNO_ACTION_ADD.equals(operation)) {
                        // 撤销添加：删除新添加的标注
                        log.debug("删除新添加的标注，annotationId: {}", annotationId);
                        deleteAnnotation(undoRedoDetail.getCurrentAnnotation().getAnnotationId(), true); // isUndoRedo=true 不重复记录事件
                    }
                } catch (Exception e) {
                    // 单个操作失败不影响其他操作，继续处理
                    log.error("撤销还原操作标注数据失败，annotation info: [{}], error: [{}]",
                            undoRedoDetail.getCurrentAnnotation(), e.getMessage());
                    continue;
                }
            }
            
            log.info("撤销处理完成");
        } else {
            log.warn("撤销事件中没有操作详情");
        }
        return Result.success();
    }

    /**
     * 重做处理
     * <p>
     * 根据重做事件中的操作类型，重新执行相应的操作：
     * - DELETE → 再次删除标注
     * - UPDATE → 应用到最新版本
     * - ADD → 再次添加标注
     * </p>
     *
     * @param event 重做事件（包含所有需要重做的操作详情）
     * @return 操作结果
     */
    private Result redoDetailHandle(UndoRedoEvent event) {
        List<UndoRedoDetail> undoRedoDetails = event.getUndoRedoDetails();
        if (CollectionUtils.isNotEmpty(undoRedoDetails)) {
            log.info("开始处理重做，操作数量: {}", undoRedoDetails.size());
            
            for (UndoRedoDetail undoRedoDetail : undoRedoDetails) {
                try {
                    String operation = undoRedoDetail.getOperation();
                    Long annotationId = undoRedoDetail.getCurrentAnnotation().getAnnotationId();
                    
                    log.debug("执行重做操作，operation: {}, annotationId: {}", operation, annotationId);
                    
                    if (Constant.ANNO_ACTION_DELETE.equals(operation)) {
                        // 重做删除：再次删除标注
                        log.debug("再次删除标注，annotationId: {}", annotationId);
                        deleteAnnotation(undoRedoDetail.getCurrentAnnotation().getAnnotationId(), true); // isUndoRedo=true 不重复记录事件
                        
                    } else if (Constant.ANNO_ACTION_UPDATE.equals(operation)) {
                        // 重做更新：应用到最新版本
                        log.debug("应用最新版本，annotationId: {}", annotationId);
                        AnnotationDTO annotationDTO = new AnnotationDTO();
                        BeanUtils.copyProperties(annotationDTO, undoRedoDetail.getCurrentAnnotation());
                        updateAnnotation(annotationDTO, true); // isUndoRedo=true 不重复记录事件
                        
                    } else if (Constant.ANNO_ACTION_ADD.equals(operation)) {
                        // 重做添加：再次添加标注
                        log.debug("再次添加标注，annotationId: {}", annotationId);
                        AnnotationDTO annotationDTO = new AnnotationDTO();
                        BeanUtils.copyProperties(annotationDTO, undoRedoDetail.getCurrentAnnotation());
                        addAnnotation(annotationDTO, true); // isUndoRedo=true 不重复记录事件
                    }
                } catch (Exception e) {
                    // 单个操作失败不影响其他操作，继续处理
                    log.error("重做操作标注数据失败，annotation info: [{}], error: [{}]",
                            undoRedoDetail.getCurrentAnnotation(), e.getMessage());
                    continue;
                }
            }
            
            log.info("重做处理完成");
        } else {
            log.warn("重做事件中没有操作详情");
        }
        return Result.success();
    }
}

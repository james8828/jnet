package com.jnet.anno.service;

import com.jnet.anno.domain.Annotation;
import com.jnet.anno.domain.SpatialAnnotation;
import com.jnet.anno.utils.annotation.UndoRedoReq;
import com.jnet.anno.vo.anno.*;
import com.jnet.api.R;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

/**
 * 基于 Hibernate Spatial 的空间分析服务接口
 * @author mu
 * @version 1.0
 * @since 2026/3/10
 */
public interface SpatialAnalysisService {

    /**
     * 获取两个标注之间的距离
     * @param req 距离计算请求参数
     * @return 距离计算结果
     * @throws Exception 异常
     */
    R<AnnotationDistanceVo> getDistance(AnnotationDistanceReq req) throws Exception;

    /**
     * 添加标注
     * @param req 标注 VO 对象
     * @return 标注实体
     * @throws Exception 异常
     */
    Annotation addAnnotation(AnnotationVo req) throws Exception;

    /**
     * 删除标注
     * @param id 标注 ID
     * @return 操作结果
     * @throws Exception 异常
     */
    R deleteAnnotation(Long id) throws Exception;

    /**
     * 更新标注
     * @param req 标注更新参数
     * @return 操作结果
     * @throws Exception 异常
     */
    R updateAnnotation(AnnotationUpdateVo req) throws Exception;

    /**
     * 填充标注（移除孔洞）
     * @param req 标注更新参数
     * @return 操作结果
     * @throws Exception 异常
     */
    R padding(AnnotationUpdateVo req) throws Exception;

    /**
     * 悬浮标注（取消关联）
     * @param req 标注更新参数
     * @return 操作结果
     * @throws Exception 异常
     */
    R stickup(AnnotationUpdateVo req) throws Exception;

    /**
     * 合并预览
     * @param annotationIds 标注 ID 列表
     * @return 合并后的几何对象
     * @throws Exception 异常
     */
    R<Geometry> mergePreview(List<Long> annotationIds) throws Exception;

    /**
     * 标注布尔运算（并集、差集等）
     * @param req 运算请求参数
     * @return 运算后的几何对象
     * @throws Exception 异常
     */
    R<Geometry> annotationOperation(AnnotationOperationReq req) throws Exception;

    /**
     * 批量操作标注
     * @param req 批量操作请求参数
     * @return 批量操作结果列表
     * @throws Exception 异常
     */
    R<List<AnnotationBatchRespVo>> batch(AnnotationBatchReq req) throws Exception;

    /**
     * 撤销操作
     * @param req 撤销请求参数
     * @return 操作结果
     * @throws Exception 异常
     */
    R undoAnnotation(UndoRedoReq req) throws Exception;

    /**
     * 重做操作
     * @param req 重做请求参数
     * @return 操作结果
     * @throws Exception 异常
     */
    R redoAnnotation(UndoRedoReq req) throws Exception;

    /**
     * 清除撤销和重做栈
     * @param req 清除请求参数
     * @return 操作结果
     * @throws Exception 异常
     */
    R clearUndoAndRedoStack(UndoRedoReq req) throws Exception;

    /**
     * 检查撤销和重做状态
     * @param req 检查请求参数
     * @return 状态信息
     * @throws Exception 异常
     */
    R checkUndoAndRedoStatus(UndoRedoReq req) throws Exception;

    /**
     * 计算两个 Geometry 的平均距离
     * @param geom1 几何对象 1
     * @param geom2 几何对象 2
     * @return 平均距离
     */
    double calculateAverageDistance(Geometry geom1, Geometry geom2);

    /**
     * 在 Geometry 上均匀采样若干点
     * @param geom 几何对象
     * @param numPoints 采样点数（-1 表示返回所有点）
     * @return 坐标点列表
     */
    List<Coordinate> sampleGeometry(Geometry geom, int numPoints);

    /**
     * 计算几何面积
     * @param geometry 几何对象
     * @return 面积
     */
    Double calculateArea(Geometry geometry);

    /**
     * 计算几何周长
     * @param geometry 几何对象
     * @return 周长
     */
    Double calculatePerimeter(Geometry geometry);

    /**
     * 查找指定区域内的所有标注
     * @param minX 最小 X 坐标
     * @param minY 最小 Y 坐标
     * @param maxX 最大 X 坐标
     * @param maxY 最大 Y 坐标
     * @param slideId 切片 ID
     * @return 标注列表
     */
    List<SpatialAnnotation> findAnnotationsInRegion(
            Double minX, Double minY, Double maxX, Double maxY, Long slideId);

    /**
     * 检测标注是否相交
     * @param annotationId1 标注 ID 1
     * @param annotationId2 标注 ID 2
     * @return 是否相交
     */
    boolean checkIntersection(Long annotationId1, Long annotationId2);

    /**
     * 合并相交的标注
     * @param annotationId1 标注 ID 1
     * @param annotationId2 标注 ID 2
     * @return 合并后的标注
     */
    SpatialAnnotation mergeAnnotations(Long annotationId1, Long annotationId2);

    /**
     * 简化几何（减少点数）
     * @param annotationId 标注 ID
     * @param tolerance 容差
     * @return 简化后的标注
     */
    SpatialAnnotation simplifyAnnotation(Long annotationId, double tolerance);
}

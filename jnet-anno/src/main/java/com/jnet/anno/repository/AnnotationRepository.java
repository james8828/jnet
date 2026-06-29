package com.jnet.anno.repository;

import com.jnet.anno.domain.Annotation;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * 基于 Hibernate Spatial 的标注数据访问层
 * 支持空间查询：包含、相交、距离等
 * @author mu
 * @version 1.0
 * @since 2026/3/10
 */


@Repository
@Transactional
public interface AnnotationRepository extends JpaRepository<Annotation, Long> {

    /**
     * 根据 ids 查询
     */
    @Query("SELECT a FROM Annotation a WHERE a.annotationId IN :ids")
    List<Annotation> findByIds(@Param("ids") List<Long> ids);

    /**
     * 空间查询：查找包含指定点的标注
     * 使用 ST_Contains 函数
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE function('ST_Contains', a.geom, :point) = true")
    List<Annotation> findByContainingPoint(@Param("point") Point point);

    /**
     * 空间查询：查找与指定几何相交的标注
     * 使用 ST_Intersects 函数
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE function('ST_Intersects', a.geom, :geometry) = true")
    List<Annotation> findByIntersectingGeometry(@Param("geometry") Geometry geometry);

    /**
     * 空间查询：查找指定距离范围内的标注
     * 使用 ST_DWithin 函数（高效，使用空间索引）
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE function('ST_DWithin', a.geom, :point, :distance) = true")
    List<Annotation> findByDistanceWithin(@Param("point") Geometry point, @Param("distance") double distance);

    /**
     * 空间查询：计算与指定点的距离
     * 使用 ST_Distance 函数
     */
    @Transactional(readOnly = true)
    @Query("SELECT a, function('ST_Distance', a.geom, :point) as distance FROM Annotation a WHERE a.slideId = :slideId ORDER BY distance")
    List<Object[]> findWithDistance(
            @Param("point") Geometry point,
            @Param("slideId") Long slideId,
            Pageable pageable);

    /**
     * 空间查询：查找在指定矩形范围内的标注
     * 使用 ST_MakeEnvelope 和 ST_Intersects
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE " +
           "a.slideId = :slideId AND " +
           "function('ST_Intersects', a.geom, " +
           "function('ST_MakeEnvelope', :minX, :minY, :maxX, :maxY)) = true")
    List<Annotation> findByBoundingBox(
            @Param("minX") Double minX,
            @Param("minY") Double minY,
            @Param("maxX") Double maxX,
            @Param("maxY") Double maxY,
            @Param("slideId") Long slideId);

    /**
     * 获取标注的面积（使用数据库计算）
     * 使用 ST_Area 函数
     */
    @Transactional(readOnly = true)
    @Query("SELECT function('ST_Area', a.geom) FROM Annotation a WHERE a.annotationId = :id")
    Double getArea(@Param("id") Long id);

    /**
     * 获取标注的长度/周长（使用数据库计算）
     * 使用 ST_Length 函数
     */
    @Transactional(readOnly = true)
    @Query("SELECT function('ST_Length', a.geom) FROM Annotation a WHERE a.annotationId = :id")
    Double getLength(@Param("id") Long id);

    /**
     * 空间查询：查找最近的 N 个标注
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE " +
           "a.slideId = :slideId " +
           "ORDER BY function('ST_Distance', a.geom, :point)")
    List<Annotation> findNearest(
            @Param("point") Geometry point,
            @Param("slideId") Long slideId,
            Pageable pageable);

    /**
     * 批量查询某个切片的所有标注
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE a.slideId = :slideId")
    List<Annotation> findBySlideId(@Param("slideId") Long slideId);

    /**
     * 根据 annotationId 查询单个标注
     */
    @Transactional(readOnly = true)
    Optional<Annotation> findByAnnotationId(Long annotationId);

    @Transactional(readOnly = true)
    Optional<Long> countBySlideIdAndCreateBy(Long slideId, Long createBy);

    @Transactional(readOnly = true)
    Optional<Long> countBySlideIdInAndCreateBy(List<Long> slideIds, Long createBy);

    @Transactional(readOnly = true)
    Optional<Long> countByTagId(Long tagId);
    
    /**
     * 根据图像ID集合查询标注（仅查询有效的标注）
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE a.imageId IN :imageIds AND a.isActive = true")
    List<Annotation> findByImageIdInAndIsActiveTrue(@Param("imageIds") List<Long> imageIds);
    
    /**
     * 根据项目ID查询标注（仅查询有效的标注）
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE a.projectId = :projectId AND a.isActive = true")
    List<Annotation> findByProjectIdAndIsActiveTrue(@Param("projectId") Long projectId);
    
    /**
     * 根据批次ID集合查询标注（仅查询有效的标注）
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE a.batchId IN :batchIds AND a.isActive = true")
    List<Annotation> findByBatchIdInAndIsActiveTrue(@Param("batchIds") List<Long> batchIds);
    
    /**
     * 根据标签ID集合查询标注（仅查询有效的标注）
     */
    @Transactional(readOnly = true)
    @Query("SELECT a FROM Annotation a WHERE a.tagId IN :tagIds AND a.isActive = true")
    List<Annotation> findByTagIdInAndIsActiveTrue(@Param("tagIds") List<Long> tagIds);
}
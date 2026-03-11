package com.jnet.anno.repository;


import com.jnet.anno.domain.SpatialMeasure;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 基于 Hibernate Spatial 的测量标注 Repository
 * @author mugw
 * @version 1.0
 * @since 2025/3/10
 */
@Repository
public interface SpatialMeasureRepository extends JpaRepository<SpatialMeasure, Long> {

    /**
     * 根据切片 ID 分页查询（排除 Point 类型）
     */
    @Query("SELECT m FROM SpatialMeasure m WHERE m.slideId = :slideId " +
            "AND m.locationType <> :locationType AND (:measureFullName IS NULL OR m.measureFullName LIKE %:measureFullName%)")
    Page<SpatialMeasure> findBySlideIdExcludingPoint(
            @Param("slideId") Long slideId,
            @Param("locationType") String locationType,
            @Param("measureFullName") String measureFullName,
            Pageable pageable);

    /**
     * 统计 Point 类型数量
     */
    @Query("SELECT COUNT(m) FROM SpatialMeasure m WHERE m.slideId = :slideId " +
            "AND m.locationType = :locationType " +
            "AND (:measureFullName IS NULL OR m.measureFullName LIKE %:measureFullName%)")
    long countPointsBySlideIdAndFilter(
            @Param("slideId") Long slideId,
            @Param("locationType") String locationType,
            @Param("measureFullName") String measureFullName);

    /**
     * 统计非 Point 类型数量
     */
    @Query("SELECT COUNT(m) FROM SpatialMeasure m WHERE m.slideId = :slideId " +
            "AND m.locationType <> :locationType " +
            "AND (:measureFullName IS NULL OR m.measureFullName LIKE %:measureFullName%)")
    long countNonPointMeasures(
            @Param("slideId") Long slideId,
            @Param("locationType") String locationType,
            @Param("measureFullName") String measureFullName);

    /**
     * 根据切片 ID 分页查询
     */
    Page<SpatialMeasure> findBySlideId(Long slideId, Pageable pageable);

    /**
     * 根据切片 ID 和类型分页查询
     */
    Page<SpatialMeasure> findBySlideIdAndLocationType(Long slideId, String locationType, Pageable pageable);

    /**
     * 根据切片 ID 统计数量
     */
    long countBySlideId(Long slideId);

    /**
     * 根据切片 ID 和类型统计数量
     */
    long countBySlideIdAndLocationType(Long slideId, String locationType);

    /**
     * 根据切片 ID 查询所有（非分页）
     */
    List<SpatialMeasure> findBySlideId(Long slideId);

    /**
     * 根据切片 ID 和类型查询（排除 Point 类型）
     */
    List<SpatialMeasure> findBySlideIdAndLocationTypeNot(Long slideId, String locationType);

    /**
     * 查找同一切片和名称的最大编号
     */
    @Query("SELECT MAX(m.number) FROM SpatialMeasure m WHERE m.slideId = :slideId AND m.measureName = :measureName")
    Long findMaxNumberBySlideIdAndMeasureName(@Param("slideId") Long slideId, @Param("measureName") String measureName);

    /**
     * 空间查询：查找与指定几何体相交的测量标注
     */
    @Query("SELECT m FROM SpatialMeasure m WHERE ST_Intersects(m.geometry, :geometry) = true")
    List<SpatialMeasure> findByGeometryIntersects(@Param("geometry") Geometry geometry);

    /**
     * 空间查询：查找在指定几何体范围内的测量标注
     */
    @Query("SELECT m FROM SpatialMeasure m WHERE ST_Within(m.geometry, :geometry) = true")
    List<SpatialMeasure> findByGeometryWithin(@Param("geometry") Geometry geometry);

    /**
     * 空间查询：查找距离指定点一定范围内的测量标注
     */
    @Query(value = "SELECT * FROM t_measure WHERE ST_DWithin(contour, :point, :distance)", nativeQuery = true)
    List<SpatialMeasure> findByGeometryNear(@Param("point") Geometry point, @Param("distance") double distance);

    @Transactional(readOnly = true)
    SpatialMeasure findByMeasureId(Long measureId);
}

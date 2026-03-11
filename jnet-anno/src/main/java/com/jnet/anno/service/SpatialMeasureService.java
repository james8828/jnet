package com.jnet.anno.service;

import com.jnet.anno.domain.Measure;
import com.jnet.anno.vo.measure.MeasureVo;
import com.jnet.api.R;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 基于 Hibernate Spatial 的测量标注管理接口
 * @author mugw
 * @version 1.0
 * @since 2025/3/10
 */
public interface SpatialMeasureService {

    /**
     * 分页查询测量标注（支持名称模糊查询）
     * @param slideId 切片 ID
     * @param measureFullName 测量全称（可选，支持模糊查询）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<MeasureVo> pageBySlideIdWithFilter(Long slideId, String measureFullName, Pageable pageable);

    /**
     * 统计 Point 类型数量（支持名称过滤）
     * @param slideId 切片 ID
     * @param measureFullName 测量全称（可选）
     * @return Point 类型数量
     */
    long countPointsBySlideIdAndFilter(Long slideId, String measureFullName);

    /**
     * 添加测量标注
     * @param req 测量标注对象
     * @return 返回添加后的测量标注
     * @throws Exception 异常
     */
    R<Measure> addMeasure(Measure req) throws Exception;

    /**
     * 删除测量标注
     * @param measureId 测量标注 ID
     * @return 操作结果
     * @throws Exception 异常
     */
    R delete(Long measureId) throws Exception;

    /**
     * 根据 ID 查询测量标注
     * @param measureId 测量标注 ID
     * @return 测量标注对象
     */
    Measure findById(Long measureId);

    /**
     * 分页查询测量标注
     * @param slideId 切片 ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<MeasureVo> pageBySlideId(Long slideId, Pageable pageable);

    /**
     * 根据切片 ID 查询测量标注
     * @param slideId 切片 ID
     * @return 测量标注列表
     */
    List<Measure> findBySlideId(Long slideId);

    /**
     * 根据类型分页查询测量标注
     * @param slideId 切片 ID
     * @param locationType 位置类型
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<MeasureVo> pageBySlideIdAndType(Long slideId, String locationType, Pageable pageable);

    /**
     * 导出测量标注数据
     * @param slideId 切片 ID
     * @throws Exception 异常
     */
    void export(Long slideId) throws Exception;

    /**
     * 统计指定切片的测量标注数量
     * @param slideId 切片 ID
     * @return 数量
     */
    long countBySlideId(Long slideId);

    /**
     * 统计指定切片和类型的测量标注数量
     * @param slideId 切片 ID
     * @param locationType 位置类型
     * @return 数量
     */
    long countBySlideIdAndType(Long slideId, String locationType);
}

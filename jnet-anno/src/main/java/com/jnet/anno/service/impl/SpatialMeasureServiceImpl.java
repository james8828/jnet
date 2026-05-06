package com.jnet.anno.service.impl;

import com.jnet.anno.constant.Constant;
import com.jnet.anno.domain.Measure;
import com.jnet.anno.domain.SpatialMeasure;
import com.jnet.anno.netty.websocket.NioWebSocketHandler;
import com.jnet.anno.repository.SpatialMeasureRepository;
import com.jnet.anno.service.SpatialMeasureService;
import com.jnet.anno.utils.MessageSourceUtil;
import com.jnet.anno.utils.SecurityUtils;
import com.jnet.anno.utils.measure.MeasureMessageGenerator;
import com.jnet.anno.vo.measure.MeasureVo;
import com.alibaba.excel.EasyExcel;
import com.jnet.common.result.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 Hibernate Spatial 的测量标注管理服务实现
 *
 * @author mugw
 * @version 1.0
 * @since 2025/3/10
 */
@Service
public class SpatialMeasureServiceImpl implements SpatialMeasureService {

    private static final Logger log = LoggerFactory.getLogger(SpatialMeasureServiceImpl.class);
    @Resource
    private SpatialMeasureRepository spatialMeasureRepository;

    @Resource
    private NioWebSocketHandler webSocketHandler;

    @Resource
    private HttpServletResponse response;


    @Override
    public Page<MeasureVo> pageBySlideIdWithFilter(Long slideId, String measureFullName, Pageable pageable) {
        Page<SpatialMeasure> measurePage = spatialMeasureRepository.findBySlideIdExcludingPoint(
                slideId,
                Geometry.TYPENAME_POINT,
                measureFullName,
                pageable);

        List<MeasureVo> measureVoList = measurePage.getContent().stream()
                .map(this::convertToVo)
                .map(this::renderUser)
                .collect(Collectors.toList());

        return new PageImpl<>(measureVoList, pageable, measurePage.getTotalElements());
    }

    @Override
    public long countPointsBySlideIdAndFilter(Long slideId, String measureFullName) {
        return spatialMeasureRepository.countPointsBySlideIdAndFilter(
                slideId,
                Geometry.TYPENAME_POINT,
                measureFullName);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<Measure> addMeasure(Measure req) throws Exception {
        if (req == null || req.getGeometry() == null || !req.getGeometry().isSimple()) {
            throw new Exception(MessageSourceUtil.getMessage("ARGUMENT_INVALID"));
        }

        req.setCreateBy(SecurityUtils.getUserId());
        req.setAnnotationType("Measure");

        long number = 1L;
        Long maxNumber = spatialMeasureRepository.findMaxNumberBySlideIdAndMeasureName(
                req.getSlideId(), req.getMeasureName());

        if (maxNumber != null) {
            number += maxNumber;
        }

        String measureFullName = req.getMeasureName() + number;
        req.setMeasureFullName(measureFullName);
        req.setNumber(number);
        SpatialMeasure measure = convertToSpatialMeasure(req);
        measure = spatialMeasureRepository.save(measure);
        req = convertToMeasure(measure);
        webSocketHandler.sendMessage(MeasureMessageGenerator.generateAnnotationMessage(req, Constant.ANNO_ACTION_ADD));
        return Result.success(req);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result delete(Long measureId) throws Exception {
        if (!Optional.ofNullable(measureId).isPresent()) {
            throw new Exception(MessageSourceUtil.getMessage("ARGUMENT_INVALID"));
        }

        SpatialMeasure measure = spatialMeasureRepository.findById(measureId)
                .orElseThrow(() -> new Exception(MessageSourceUtil.getMessage("NO_ANNOTATION_DATA")));

        spatialMeasureRepository.delete(measure);

        webSocketHandler.sendMessage(MeasureMessageGenerator.generateAnnotationMessage(convertToMeasure(measure), Constant.ANNO_ACTION_DELETE));
        return Result.success(MessageSourceUtil.getMessage("OPERATE_SUCCEED"), null);
    }

    @Override
    public Measure findById(Long measureId) {
        Measure measure = null;
        try {
            SpatialMeasure spatialMeasure = spatialMeasureRepository.findById(measureId)
                    .orElseThrow(() -> new Exception(MessageSourceUtil.getMessage("NO_ANNOTATION_DATA")));
            measure = convertToMeasure(spatialMeasure);
        } catch (Exception e) {
            log.error("[SpatialMeasureServiceImpl.findById]", e);
        }
        return measure;
    }

    @Override
    public Page<MeasureVo> pageBySlideId(Long slideId, Pageable pageable) {
        Page<SpatialMeasure> measurePage = spatialMeasureRepository.findBySlideId(slideId, pageable);
        List<MeasureVo> measureVoList = measurePage.getContent().stream()
                .map(this::convertToVo)
                .map(this::renderUser)
                .collect(Collectors.toList());

        return new PageImpl<>(measureVoList, pageable, measurePage.getTotalElements());
    }

    @Override
    public List<Measure> findBySlideId(Long slideId) {
        return spatialMeasureRepository.findBySlideId(slideId).stream()
                .map(this::convertToMeasure)
                .collect(Collectors.toList());

    }

    @Override
    public Page<MeasureVo> pageBySlideIdAndType(Long slideId, String locationType, Pageable pageable) {
        Page<SpatialMeasure> measurePage;

        if (locationType != null && !locationType.isEmpty()) {
            measurePage = spatialMeasureRepository.findBySlideIdAndLocationType(slideId, locationType, pageable);
        } else {
            measurePage = spatialMeasureRepository.findBySlideId(slideId, pageable);
        }

        List<MeasureVo> measureVoList = measurePage.getContent().stream()
                .map(this::convertToVo)
                .map(this::renderUser)
                .collect(Collectors.toList());

        return new PageImpl<>(measureVoList, pageable, measurePage.getTotalElements());
    }

    @Override
    public void export(Long slideId) throws Exception {
        List<SpatialMeasure> measureList = spatialMeasureRepository.findBySlideIdAndLocationTypeNot(slideId, Geometry.TYPENAME_POINT);

        List<MeasureVo> measureVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(measureList)) {
            measureVoList = measureList.stream()
                    .map(this::convertToVo)
                    .map(this::renderUser)
                    .collect(Collectors.toList());
        }

        long points = spatialMeasureRepository.countBySlideIdAndLocationType(slideId, Geometry.TYPENAME_POINT);
        if (points > 0) {
            measureVoList.add(MeasureVo.builder()
                    .pointCount(points)
                    .measureFullName("P")
                    .build());
        }

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String exportName = URLEncoder.encode(MessageSourceUtil.getMessage("EXCEL_TITLE"), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + exportName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), MeasureVo.class)
                .sheet(exportName)
                .doWrite(measureVoList);
    }

    @Override
    public long countBySlideId(Long slideId) {
        return spatialMeasureRepository.countBySlideId(slideId);
    }

    @Override
    public long countBySlideIdAndType(Long slideId, String locationType) {
        return spatialMeasureRepository.countBySlideIdAndLocationType(slideId, locationType);
    }

    /**
     * 转换为 VO 对象
     */
    private MeasureVo convertToVo(SpatialMeasure measure) {
        if (measure == null) {
            return null;
        }
        MeasureVo measureVo = new MeasureVo();
        BeanUtils.copyProperties(measure, measureVo);
        return measureVo;
    }

    /**
     * 填充用户信息
     */
    private MeasureVo renderUser(MeasureVo measureVo) {
        String userName = "anno";
        measureVo.setCreateUserName(userName);
        return measureVo;
    }

    /**
     * 将 Measure 转换为 SpatialMeasure
     *
     * @param measure 原始 Measure 对象
     * @return 转换后的 SpatialMeasure 对象
     */
    private SpatialMeasure convertToSpatialMeasure(Measure measure) {
        if (measure == null) {
            return null;
        }

        SpatialMeasure spatialMeasure = new SpatialMeasure();
        BeanUtils.copyProperties(measure, spatialMeasure);

        return spatialMeasure;
    }

    /**
     * 将 SpatialMeasure 转换为 Measure
     *
     * @param spatialMeasure SpatialMeasure 对象
     * @return 转换后的 Measure 对象
     */
    private Measure convertToMeasure(SpatialMeasure spatialMeasure) {
        if (spatialMeasure == null) {
            return null;
        }

        Measure measure = new Measure();
        BeanUtils.copyProperties(spatialMeasure, measure);

        return measure;
    }
}

package com.jnet.anno.controller;

import com.jnet.anno.domain.Measure;
import com.jnet.anno.netty.message.AnnotationFeature;
import com.jnet.anno.service.SpatialMeasureService;
import com.jnet.anno.utils.measure.MeasureMessageGenerator;
import com.jnet.anno.vo.measure.MeasureAddVo;
import com.jnet.anno.vo.measure.MeasureReq;
import com.jnet.anno.vo.measure.MeasureVo;
import com.jnet.common.constant.ResultCode;
import com.jnet.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mugw
 * @version 1.0
 * @description 测量标注管理
 * @date 2025/5/21 14:33:43
 */
@Tag(name = "viewer页面-测量")
@Slf4j
@RestController
@RequestMapping("/api/v1/measure")
public class MeasureController {

    @Resource
    private SpatialMeasureService spatialMeasureService;

    @Operation(summary = "获取测量分页")
    @PostMapping("/page")
    public Result<Page<MeasureVo>> page(@Validated @RequestBody MeasureReq req) throws Exception {
        PageRequest pageRequest = PageRequest.of(req.getCurrent().intValue() - 1, req.getSize().intValue());

        Page<MeasureVo> measurePage = spatialMeasureService.pageBySlideIdWithFilter(
                req.getSlideId(),
                req.getMeasureFullName(),
                pageRequest);

        List<MeasureVo> measureVoList = new ArrayList<>(measurePage.getContent());

        long points = spatialMeasureService.countPointsBySlideIdAndFilter(
                req.getSlideId(),
                req.getMeasureFullName());

        boolean hasPoints = points > 0;
        long totalElements = measurePage.getTotalElements() + (hasPoints ? 1 : 0);

        if (hasPoints) {
            measureVoList.add(MeasureVo.builder()
                    .pointCount(points)
                    .measureFullName("P")
                    .build());
        }

        Page<MeasureVo> pageResult = new PageImpl<>(measureVoList, pageRequest, totalElements);

        return Result.success(pageResult);
    }


    @Operation(summary = "获取GeoJson数据")
    @GetMapping("/getDataList")
    public Result<List<AnnotationFeature>> getDataList(@RequestParam(value = "slideId") Long slideId) throws Exception {
        List<Measure> measureList = spatialMeasureService.findBySlideId(slideId);
        List<AnnotationFeature> featureList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(measureList)) {
            featureList = measureList.stream().map(MeasureMessageGenerator::generateFeatures).collect(Collectors.toList());
        }
        return Result.success(featureList);
    }

    @Operation(summary = "添加测量")
    @PostMapping("/add")
    public Result<Long> add(@Validated @RequestBody MeasureAddVo measureAddVo) throws Exception {
        Measure measure = new Measure();
        BeanUtils.copyProperties(measure, measureAddVo);
        Result<Measure> result = spatialMeasureService.addMeasure(measure);
        if (result.getCode() == ResultCode.SUCCESS.getCode()) {
            return Result.success(result.getData().getMeasureId());
        } else {
            return Result.error(ResultCode.FAIL);
        }
    }

    @Operation(summary = "删除测量")
    @PostMapping("/del")
    public Result<String> del(@RequestParam(value = "marking_id") Long measureId) throws Exception {
        return spatialMeasureService.delete(measureId);
    }


    @Operation(summary = "标注测量excel导出")
    @GetMapping("/export")
    public void export(@RequestParam(value = "slideId") Long slideId) throws Exception {
        spatialMeasureService.export(slideId);
    }
}

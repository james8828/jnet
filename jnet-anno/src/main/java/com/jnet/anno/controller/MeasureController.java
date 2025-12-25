package com.jnet.anno.controller;

import com.jnet.anno.domain.Measure;
import com.jnet.anno.netty.message.AnnotationFeature;
import com.jnet.anno.service.MeasureService;
import com.jnet.anno.utils.measure.MeasureMessageGenerator;
import com.jnet.anno.vo.measure.MeasureAddVo;
import com.jnet.anno.vo.measure.MeasureReq;
import com.jnet.anno.vo.measure.MeasureVo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jnet.api.R;
import com.jnet.api.vo.CustomPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.Geometry;
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
@RequestMapping("/measure")
public class MeasureController {

    @Resource
    private MeasureService measureService;

    @Operation(summary = "获取测量分页")
    @PostMapping("/page")
    public R<CustomPage<MeasureVo>> page(@Validated @RequestBody MeasureReq req) throws Exception {
        CustomPage<Measure> page = new CustomPage<>(req);
        measureService.page(page, Wrappers.<Measure>lambdaQuery().eq(Measure::getSlideId, req.getSlideId())
                .like(req.getMeasureFullName() != null, Measure::getMeasureFullName, req.getMeasureFullName())
                .ne(Measure::getLocationType, Geometry.TYPENAME_POINT));
        long points = measureService.count(Wrappers.<Measure>lambdaQuery().eq(Measure::getSlideId, req.getSlideId())
                .like(req.getMeasureFullName() != null, Measure::getMeasureFullName, req.getMeasureFullName())
                .eq(Measure::getLocationType, Geometry.TYPENAME_POINT));
        CustomPage<MeasureVo> pageResult = new CustomPage<>(req);
        if (page.getTotal() > 0) {
            List<MeasureVo> measureVoList = page.getRecords().stream().map(MeasureVo::convert).collect(Collectors.toList());
            if (points > 0) {
                measureVoList.add(MeasureVo.builder().pointCount(points).measureFullName("P").build());
            }
            pageResult.setTotal(page.getTotal());
            pageResult.setRecords(measureVoList);
        }
        return R.success(pageResult);
    }

    @Operation(summary = "获取GeoJson数据")
    @GetMapping("/getDataList")
    public R<List<AnnotationFeature>> getDataList(@RequestParam(value = "slideId") Long slideId) throws Exception {
        List<Measure> measureList = measureService.list(Wrappers.<Measure>lambdaQuery().eq(Measure::getSlideId, slideId));
        List<AnnotationFeature> featureList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(measureList)){
            featureList = measureList.stream().map(MeasureMessageGenerator::generateFeatures).collect(Collectors.toList());
        }
        return R.success(featureList);
    }

    @Operation(summary = "添加测量")
    @PostMapping("/add")
    public R<Long> add(@Validated @RequestBody MeasureAddVo measureAddVo) throws Exception {
        Measure measure = new Measure();
        BeanUtils.copyProperties(measure, measureAddVo);
        R<Measure> result = measureService.addMeasure(measure);
        if (result.getCode() == R.SUCCESS){
            return R.success(result.getData().getMeasureId());
        }else{
            return R.fail(result.getMsg());
        }
    }

    @Operation(summary = "删除测量")
    @PostMapping("/del")
    public R<String> del(@RequestParam(value = "marking_id") Long measureId) throws Exception {
        return measureService.delete(measureId);
    }


    @Operation(summary = "标注测量excel导出")
    @GetMapping("/export")
    public void export(@RequestParam(value = "slideId") Long slideId) throws Exception {
        measureService.export(slideId);
    }

}

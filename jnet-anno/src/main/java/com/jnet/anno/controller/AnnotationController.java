package com.jnet.anno.controller;

import com.jnet.anno.domain.Annotation;
import com.jnet.anno.netty.message.AnnotationFeature;
import com.jnet.anno.service.AnnotationService;
import com.jnet.anno.utils.annotation.AnnotationMessageGenerator;
import com.jnet.anno.utils.annotation.UndoRedoReq;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jnet.anno.vo.anno.*;
import com.jnet.api.R;
import com.jnet.common.core.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.Geometry;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description 标注管理
 * @date 2025/5/21 14:33:43
 */
@Tag(name = "标注管理")
@RestController
@RequestMapping("/annotation")
public class AnnotationController {

    @Resource
    private AnnotationService annotationService;

    @Operation(summary = "添加标注", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/insert")
    public R<String> addAnnotation(@Validated @RequestBody AnnotationAddReq annotationAddVo) throws Exception {
        AnnotationVo req = new AnnotationVo();

        BeanUtils.copyProperties(req, annotationAddVo);
        Annotation annotation = annotationService.addAnnotation(req);
        return R.success(String.valueOf(annotation.getAnnotationId()));
    }

    @PostMapping("/getDistance")
    @Operation(summary = "获取间距", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    public R<AnnotationDistanceVo> getDistance(@Validated @RequestBody AnnotationDistanceReq req) throws Exception {
        return annotationService.getDistance(req);
    }

    @Operation(summary = "删除标注", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/delete")
    public R<String> deleteAnnotation(@RequestBody AnnotationVo req) throws Exception {
        return annotationService.deleteAnnotation(req.getAnnotationId());
    }

    @Operation(summary = "更新标注", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/update")
    public R<String> updateAnnotation(@Validated @RequestBody AnnotationUpdateVo req) throws Exception {
        return annotationService.updateAnnotation(req);
    }

    @Operation(summary = "填充轮廓", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/padding")
    public R<String> padding(@Validated @RequestBody AnnotationUpdateVo req) throws Exception {
        return annotationService.padding(req);
    }

    @Operation(summary = "复制/粘贴轮廓", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/stickup")
    public R<String> stickup(@Validated @RequestBody AnnotationUpdateVo req) throws Exception {
        return annotationService.stickup(req);
    }

    @Operation(summary = "轮廓合并预览", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/mergePreview")
    public R<Geometry> mergePreview(@RequestBody AnnotationMergePreviewReq req) throws Exception {
        List<Long> annotationIds = req.getMarkingIdList();
        return annotationService.mergePreview(annotationIds);
    }

    @Operation(summary = "获取GeoJson数据", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/selectLists")
    public R<List<AnnotationFeature>> selectLists(@Validated @RequestBody AnnotationReq req) throws Exception {
        List<Annotation> annotations = annotationService.list(Wrappers.<Annotation>lambdaQuery().eq(Annotation::getSlideId, req.getSlideId()));
        List<AnnotationFeature> resp = CollectionUtils.isEmpty(annotations) ? new ArrayList<>() : AnnotationMessageGenerator.generateFeatures(annotations);
        return R.success(resp);
    }

    @Operation(summary = "合并、裁剪轮廓", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/updateOperation")
    public R<Geometry> annotationOperation(@Validated @RequestBody AnnotationOperationReq req) throws Exception {
        return annotationService.annotationOperation(req);
    }

    @Operation(summary = "批量操作", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/batch")
    public R<List<AnnotationBatchRespVo>> batch(@Validated @RequestBody AnnotationBatchReq req) throws Exception {
        return annotationService.batch(req);
    }

    @Operation(summary = "查询标签是否被使用", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/checkTagUsageStatus")
    public R<Boolean> checkTagUsageStatus(@RequestParam("id") Long id) throws Exception {
        long count = annotationService.count(Wrappers.<Annotation>lambdaQuery().eq(Annotation::getTagId, id));
        return R.success(count > 0);
    }

    @Operation(summary = "检查是否存在用户的标注数据", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/checkUserOperation")
    public R<Boolean> checkUserOperation(@RequestBody CheckUserOperation req) throws Exception {
        long count = annotationService.count(Wrappers.<Annotation>lambdaQuery()
                .eq(Annotation::getCreateBy, req.getUserId())
                .in(Annotation::getSlideId, req.getSlideId()));
        return R.success(count > 0);
    }

    @Operation(summary = "撤销", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/undoAnnotation/{slideId}")
    public R<Boolean> undoAnnotation(@PathVariable Long slideId) throws Exception {
        return annotationService.undoAnnotation(UndoRedoReq.builder().slideId(slideId).userId(SecurityUtils.getUserId()).build());
    }

    @Operation(summary = "还原", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/redoAnnotation/{slideId}")
    public R<Boolean> redo(@PathVariable Long slideId) throws Exception {
        return annotationService.redoAnnotation(UndoRedoReq.builder().slideId(slideId).userId(SecurityUtils.getUserId()).build());
    }

    @Operation(summary = "清除撤销、还原操作", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/clear/{slideId}")
    public R<Boolean> clearUndoAndRedoStack(@PathVariable Long slideId) throws Exception {
        return annotationService.clearUndoAndRedoStack(UndoRedoReq.builder().slideId(slideId).userId(SecurityUtils.getUserId()).build());
    }

    @Operation(summary = "检查撤销、还原状态", description = "返回用户列表", responses = {
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = AnnotationAddReq.class))),
            @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/checkUndoAndRedoStatus/{slideId}")
    public R<Boolean> checkUndoAndRedoStatus(@PathVariable Long slideId) throws Exception {
        return annotationService.checkUndoAndRedoStatus(UndoRedoReq.builder().slideId(slideId).userId(SecurityUtils.getUserId()).build());
    }

    @GetMapping("exportGeoJson")
    public void exportGeoJson(@RequestParam Long slideId) throws Exception {

//        return annotationService.exportGeoJson(slideId);
    }

}

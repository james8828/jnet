package com.jnet.biz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.biz.dto.AnnotationProgressDTO;
import com.jnet.biz.dto.BatchSelectImagesDTO;
import com.jnet.biz.dto.ChunkUploadDTO;
import com.jnet.biz.dto.ChunkUploadInitDTO;
import com.jnet.biz.dto.ImageQueryDTO;
import com.jnet.biz.dto.ImageStatusDTO;
import com.jnet.biz.entity.Image;
import com.jnet.biz.service.IChunkUploadService;
import com.jnet.biz.service.IImageService;
import com.jnet.biz.service.IImageTileService;
import com.jnet.biz.vo.ChunkUploadVO;
import com.jnet.biz.vo.ImageMetadataVO;
import com.jnet.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 图像资产管理 Controller
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Tag(name = "图像管理", description = "病理图像资产相关接口")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final IImageService imageService;
    private final IChunkUploadService chunkUploadService;
    private final IImageTileService imageTileService;

    /**
     * 高级检索图像
     */
    @Operation(summary = "高级检索图像", description = "支持按批次、状态、病理号、标签等多条件组合查询")
    @PostMapping("/search")
    public Result<Page<Image>> searchImages(
            @Parameter(description = "查询条件", required = true) @RequestBody ImageQueryDTO query) {
        Page<Image> page = imageService.searchImages(query);
        return Result.success(page);
    }

    /**
     * 分页查询图像列表
     */
    @Operation(summary = "分页查询图像列表", description = "基础分页查询，支持简单筛选")
    @PostMapping("/page")
    public Result<Page<Image>> listImages(
            @Parameter(description = "查询条件", required = true) @RequestBody ImageQueryDTO query) {
        Page<Image> page = imageService.searchImages(query);
        return Result.success(page);
    }

    /**
     * 获取图像详情
     */
    @Operation(summary = "获取图像详情", description = "根据ID获取图像完整信息")
    @GetMapping("/{id}")
    public Result<Image> getImage(@Parameter(description = "图像ID", required = true, example = "1") @PathVariable("id") Long id) {
        Image image = imageService.getById(id);
        if (image == null) {
            return Result.error(404, "图像不存在");
        }
        return Result.success(image);
    }

    /**
     * 更新图像生命周期状态
     */
    @Operation(summary = "更新图像生命周期状态", description = "更新图像的标注进度状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "图像ID", required = true, example = "1") @PathVariable("id") Long id,
            @Parameter(description = "状态信息", required = true) @RequestBody @Validated ImageStatusDTO dto) {
        boolean success = imageService.updateLifecycleStatus(id, dto.getStatus());
        if (success) {
            return Result.success("状态更新成功", null);
        } else {
            return Result.error("状态更新失败");
        }
    }

    /**
     * 批量更新标注进度
     */
    @Operation(summary = "批量更新标注进度", description = "批量更新多个图像的标注完成度")
    @PutMapping("/annotation-progress")
    public Result<Void> updateAnnotationProgress(
            @Parameter(description = "进度信息", required = true) @RequestBody @Validated AnnotationProgressDTO dto) {
        boolean success = imageService.batchUpdateAnnotationProgress(dto.getImageIds(), dto.getProgress());
        if (success) {
            return Result.success("更新成功", null);
        } else {
            return Result.error("更新失败");
        }
    }

    // ==================== 分片上传相关接口 ====================

    /**
     * 初始化分片上传
     */
    @Operation(summary = "初始化分片上传", description = "创建上传任务，支持秒传检测")
    @PostMapping("/chunk/init")
    public Result<ChunkUploadVO> initChunkUpload(
            @Parameter(description = "上传初始化参数", required = true) 
            @RequestBody @Validated ChunkUploadInitDTO initDTO) {
        ChunkUploadVO vo = chunkUploadService.initUpload(initDTO);
        return Result.success(vo);
    }

    /**
     * 上传分片
     */
    @Operation(summary = "上传分片", description = "上传单个分片文件")
    @PostMapping(value = "/chunk/upload", consumes = "multipart/form-data")
    public Result<Boolean> uploadChunk(
            @ModelAttribute @Validated ChunkUploadDTO uploadDTO) {
        Boolean success = chunkUploadService.uploadChunk(uploadDTO);
        return Result.success(success);
    }

    /**
     * 合并分片
     */
    @Operation(summary = "合并分片", description = "所有分片上传完成后，合并为完整文件")
    @PostMapping("/chunk/merge")
    public Result<Long> mergeChunks(
            @Parameter(description = "文件MD5", required = true, example = "d41d8cd98f00b204e9800998ecf8427e") 
            @RequestParam("fileMd5") String fileMd5,
            @Parameter(description = "批次ID", required = true, example = "1") 
            @RequestParam("batchId") Long batchId,
            @Parameter(description = "原始文件名", required = true, example = "test.svs") 
            @RequestParam("filename") String filename,
            @Parameter(description = "病理报告号", example = "P2024-001") 
            @RequestParam(value = "pathologyId", required = false) String pathologyId,
            @Parameter(description = "患者ID", example = "PATIENT_001") 
            @RequestParam(value = "patientId", required = false) String patientId) {
        Long imageId = chunkUploadService.mergeChunks(fileMd5, batchId, filename, pathologyId, patientId);
        return Result.success("合并成功", imageId);
    }

    /**
     * 取消上传
     */
    @Operation(summary = "取消上传", description = "取消上传并清理临时文件")
    @DeleteMapping("/chunk/cancel")
    public Result<Void> cancelUpload(
            @Parameter(description = "文件MD5", required = true, example = "d41d8cd98f00b204e9800998ecf8427e") 
            @RequestParam("fileMd5") String fileMd5) {
        chunkUploadService.cancelUpload(fileMd5);
        return Result.success("已取消", null);
    }

    /**
     * 批量选择切片
     */
    @Operation(summary = "批量选择切片", description = "批量将图像移动到指定批次或从imageStore中选择")
    @PostMapping("/batch-select")
    public Result<Void> batchSelectImages(
            @Parameter(description = "批量选择参数", required = true) 
            @RequestBody @Validated BatchSelectImagesDTO dto) {
        boolean success = imageService.batchSelectImages(dto);
        if (success) {
            return Result.success("操作成功", null);
        } else {
            return Result.error("操作失败");
        }
    }

    // ==================== 缩略图和瓦片相关接口 ====================

    /**
     * 获取图像元数据
     */
    @Operation(summary = "获取图像元数据", description = "获取WSI图像的金字塔层级、分辨率等元数据信息")
    @GetMapping("/{id}/metadata")
    public Result<ImageMetadataVO> getImageMetadata(
            @Parameter(description = "图像ID", required = true, example = "1") @PathVariable("id") Long id) {
        ImageMetadataVO metadata = imageTileService.getImageMetadata(id);
        return Result.success(metadata);
    }

    /**
     * 获取缩略图
     */
    @Operation(summary = "获取缩略图", description = "获取WSI图像的缩略图，支持自定义尺寸")
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(
            @Parameter(description = "图像ID", required = true, example = "1") @PathVariable("id") Long id,
            @Parameter(description = "最大尺寸（宽或高）", example = "512") 
            @RequestParam(required = false, defaultValue = "512") Integer maxSize) {
        Resource thumbnail = imageTileService.getThumbnail(id, maxSize);
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"thumbnail_" + id + ".jpg\"")
                .body(thumbnail);
    }

    /**
     * 获取Tile瓦片（Zoomify格式，用于OpenLayers Zoomify源）
     * URL格式: /api/v1/images/{id}/tiles/TileGroup{N}/{z}-{x}-{y}.jpg?tileSize=256
     * 注意：TileGroup N 是 Zoomify 的分组机制，后端忽略该参数，直接使用 z-x-y
     */
    @Operation(summary = "获取Tile瓦片（Zoomify格式）", description = "根据Zoomify路径格式获取瓦片，支持动态TileGroup分组")
    @GetMapping("/{id}/tiles/TileGroup{group}/{z}-{x}-{y}.jpg")
    public ResponseEntity<Resource> getTileByZoomifyPath(
            @Parameter(description = "图像ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "TileGroup编号（忽略）", required = true) @PathVariable("group") String group,
            @Parameter(description = "Zoom级别", required = true) @PathVariable("z") Integer zoom,
            @Parameter(description = "瓦片X坐标", required = true) @PathVariable("x") Integer x,
            @Parameter(description = "瓦片Y坐标", required = true) @PathVariable("y") Integer y,
            @Parameter(description = "瓦片尺寸（像素）", example = "256") @RequestParam(value = "tileSize", required = false, defaultValue = "256") Integer tileSize) {

        // 后端负责将zoom转换为WSI level
        Resource tile = imageTileService.getTileByZoom(id, zoom, x, y, tileSize);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400") // 缓存24小时
                .body(tile);
    }

    /**
     * 获取金字塔层级信息
     */
    @Operation(summary = "获取金字塔层级信息", description = "获取WSI图像的所有缩放层级信息")
    @GetMapping("/{id}/levels")
    public Result<String> getLevelInfo(
            @Parameter(description = "图像ID", required = true, example = "1") @PathVariable("id") Long id) {
        String levelInfo = imageTileService.getLevelInfo(id);
        return Result.success(levelInfo);
    }
}

package com.jnet.biz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.biz.dto.BatchDTO;
import com.jnet.biz.dto.BatchQueryDTO;
import com.jnet.biz.entity.Batch;
import com.jnet.biz.entity.Project;
import com.jnet.biz.service.IBatchService;
import com.jnet.biz.service.IProjectService;
import com.jnet.biz.util.BeanConverter;
import com.jnet.biz.vo.BatchVO;
import com.jnet.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批次管理 Controller
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Tag(name = "批次管理", description = "病理图像采集批次相关接口")
@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final IBatchService batchService;
    private final IProjectService projectService;

    /**
     * 分页查询批次列表
     */
    @Operation(summary = "分页查询批次列表", description = "支持按项目、批次编号、上传状态等多条件筛选")
    @PostMapping("/page")
    public Result<Page<BatchVO>> listBatches(
            @Parameter(description = "查询条件", required = true) @RequestBody BatchQueryDTO query) {
        Page<Batch> page = batchService.pageBatches(query);
        
        // 转换为VO
        Page<BatchVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<BatchVO> voList = BeanConverter.toVOList(page.getRecords(), BatchVO.class);
        
        // 填充项目名称
        if (!voList.isEmpty()) {
            // 收集所有需要查询的项目ID
            List<Long> projectIds = voList.stream()
                .map(BatchVO::getProjectId)
                .distinct()
                .collect(Collectors.toList());
            
            // 批量查询项目信息
            if (!projectIds.isEmpty()) {
                List<Project> projects = projectService.listByIds(projectIds);
                Map<Long, String> projectNameMap = projects.stream()
                    .collect(Collectors.toMap(Project::getProjectId, Project::getName));
                
                // 设置项目名称
                voList.forEach(vo -> {
                    if (vo.getProjectId() != null) {
                        vo.setProjectName(projectNameMap.get(vo.getProjectId()));
                    }
                });
            }
        }
        
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    /**
     * 获取项目下的所有批次
     */
    @Operation(summary = "获取项目下的批次列表", description = "获取指定项目的所有批次（不分页）")
    @GetMapping("/by-project/{projectId}")
    public Result<List<BatchVO>> listByProject(
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable("projectId") Long projectId) {
        List<Batch> batches = batchService.listByProjectId(projectId);
        List<BatchVO> voList = BeanConverter.toVOList(batches, BatchVO.class);
        
        // 填充项目名称
        if (!voList.isEmpty()) {
            Project project = projectService.getById(projectId);
            if (project != null) {
                voList.forEach(vo -> vo.setProjectName(project.getName()));
            }
        }
        
        return Result.success(voList);
    }

    /**
     * 获取批次详情
     */
    @Operation(summary = "获取批次详情", description = "根据批次ID获取详细信息")
    @GetMapping("/{id}")
    public Result<BatchVO> getBatch(
            @Parameter(description = "批次ID", required = true, example = "1") @PathVariable("id") Long id) {
        Batch batch = batchService.getById(id);
        if (batch == null) {
            return Result.error(404, "批次不存在");
        }
        BatchVO vo = BeanConverter.toVO(batch, BatchVO.class);
        
        // 填充项目名称
        if (batch.getProjectId() != null) {
            Project project = projectService.getById(batch.getProjectId());
            if (project != null) {
                vo.setProjectName(project.getName());
            }
        }
        
        return Result.success(vo);
    }

    /**
     * 创建批次
     */
    @Operation(summary = "创建批次", description = "创建新的图像采集批次")
    @PostMapping
    public Result<BatchVO> createBatch(@Parameter(description = "批次信息", required = true) @RequestBody @Validated BatchDTO dto) {
        Batch batch = BeanConverter.toEntity(dto, Batch.class);
        boolean success = batchService.createBatch(batch);
        if (success) {
            BatchVO vo = BeanConverter.toVO(batch, BatchVO.class);
            return Result.success("创建成功", vo);
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * 更新批次
     */
    @Operation(summary = "更新批次", description = "更新批次基本信息")
    @PutMapping("/{id}")
    public Result<Void> updateBatch(
            @Parameter(description = "批次ID", required = true, example = "1") @PathVariable("id") Long id,
            @Parameter(description = "批次信息", required = true) @RequestBody @Validated BatchDTO dto) {
        Batch batch = BeanConverter.toEntity(dto, Batch.class);
        batch.setBatchId(id);
        boolean success = batchService.updateById(batch);
        if (success) {
            return Result.success("更新成功", null);
        } else {
            return Result.error("更新失败");
        }
    }
}

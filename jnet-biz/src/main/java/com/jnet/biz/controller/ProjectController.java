package com.jnet.biz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.biz.dto.ProjectDTO;
import com.jnet.biz.dto.ProjectQueryDTO;
import com.jnet.biz.entity.Project;
import com.jnet.biz.service.IProjectService;
import com.jnet.biz.util.BeanConverter;
import com.jnet.biz.vo.ProjectVO;
import com.jnet.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 项目管理 Controller
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Tag(name = "项目管理", description = "病理AI数据池项目相关接口")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService projectService;

    /**
     * 分页查询项目列表
     */
    @Operation(summary = "分页查询项目列表", description = "支持按名称、状态、负责人等多条件筛选")
    @PostMapping("/page")
    public Result<Page<ProjectVO>> listProjects(
            @Parameter(description = "查询条件", required = true) @RequestBody ProjectQueryDTO query) {
        Page<Project> page = projectService.pageProjects(query);
        
        // 转换为VO
        Page<ProjectVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(BeanConverter.toVOList(page.getRecords(), ProjectVO.class));
        
        return Result.success(voPage);
    }

    /**
     * 获取项目详情
     */
    @Operation(summary = "获取项目详情", description = "根据项目ID获取详细信息")
    @GetMapping("/{id}")
    public Result<ProjectVO> getProject(
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable("id") Long id) {
        Project project = projectService.getById(id);
        if (project == null) {
            return Result.error(404, "项目不存在");
        }
        ProjectVO vo = BeanConverter.toVO(project, ProjectVO.class);
        return Result.success(vo);
    }

    /**
     * 创建项目
     */
    @Operation(summary = "创建项目", description = "创建新的病理分析项目")
    @PostMapping
    public Result<ProjectVO> createProject(
            @Parameter(description = "项目信息", required = true) @RequestBody @Validated ProjectDTO dto) {
        Project project = BeanConverter.toEntity(dto, Project.class);
        boolean success = projectService.createProject(project);
        if (success) {
            ProjectVO vo = BeanConverter.toVO(project, ProjectVO.class);
            return Result.success("创建成功", vo);
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * 更新项目
     */
    @Operation(summary = "更新项目", description = "更新项目基本信息")
    @PutMapping("/{id}")
    public Result<Void> updateProject(
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable("id") Long id,
            @Parameter(description = "项目信息", required = true) @RequestBody @Validated ProjectDTO dto) {
        Project project = BeanConverter.toEntity(dto, Project.class);
        project.setProjectId(id);
        boolean success = projectService.updateProject(project);
        if (success) {
            return Result.success("更新成功", null);
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 归档项目
     */
    @Operation(summary = "归档项目", description = "逻辑删除项目（软删除）")
    @DeleteMapping("/{id}")
    public Result<Void> archiveProject(
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable("id") Long id) {
        boolean success = projectService.archiveProject(id);
        if (success) {
            return Result.success("归档成功", null);
        } else {
            return Result.error("归档失败");
        }
    }

    /**
     * 获取项目统计信息
     */
    @Operation(summary = "获取项目统计信息", description = "获取项目下的批次、图像等统计数据")
    @GetMapping("/{id}/stats")
    public Result<String> getProjectStats(
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable("id") Long id) {
        String stats = projectService.getProjectStats(id);
        return Result.success(stats);
    }
}

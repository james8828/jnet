package com.jnet.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.biz.dto.ProjectQueryDTO;
import com.jnet.biz.entity.Project;
import com.jnet.biz.enums.ProjectStatus;
import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import com.jnet.biz.mapper.ProjectMapper;
import com.jnet.biz.service.IProjectService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 项目管理 Service 实现类
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements IProjectService {

    @Override
    public Page<Project> pageProjects(ProjectQueryDTO query) {
        // 验证分页参数
        query.validate();
        
        Page<Project> page = query.toPage();
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        
        // 项目名称模糊查询
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Project::getName, query.getName());
        }
        
        // 项目编码精确查询
        if (StringUtils.hasText(query.getCode())) {
            wrapper.eq(Project::getCode, query.getCode());
        }
        
        // 项目状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(Project::getStatus, query.getStatus());
        }
        
        // 负责人ID筛选
        if (query.getManagerId() != null) {
            wrapper.eq(Project::getManagerId, query.getManagerId());
        }
        
        // 隐私级别筛选
        if (query.getPrivacyLevel() != null) {
            wrapper.eq(Project::getPrivacyLevel, query.getPrivacyLevel());
        }
        
        // 排序处理
        if (StringUtils.hasText(query.getOrderBy())) {
            if ("asc".equalsIgnoreCase(query.getOrderDirection())) {
                wrapper.orderByAsc(getOrderColumn(query.getOrderBy()));
            } else {
                wrapper.orderByDesc(getOrderColumn(query.getOrderBy()));
            }
        } else {
            wrapper.orderByDesc(Project::getCreateTime);
        }
        
        return this.page(page, wrapper);
    }

    /**
     * 获取排序字段（防止SQL注入）
     */
    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<Project, ?> getOrderColumn(String orderBy) {
        return switch (orderBy.toLowerCase()) {
            case "name" -> Project::getName;
            case "code" -> Project::getCode;
            case "create_time" -> Project::getCreateTime;
            case "update_time" -> Project::getUpdateTime;
            default -> Project::getCreateTime; // 默认按创建时间排序
        };
    }

    @Override
    public boolean createProject(Project project) {
        // 检查项目编码是否重复
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getCode, project.getCode());
        if (this.count(wrapper) > 0) {
            throw new BizException(BizErrorCode.PROJECT_CODE_EXISTS, 
                    "项目编码已存在: " + project.getCode());
        }
        
        // 设置默认值
        if (!StringUtils.hasText(project.getStatus())) {
            project.setStatus(ProjectStatus.ACTIVE.getCode()); // 默认为 ACTIVE
        }
        if (project.getPrivacyLevel() == null) {
            project.setPrivacyLevel(1); // 默认为公开
        }
        
        // 手动设置审计字段
        LocalDateTime now = LocalDateTime.now();
        project.setCreateTime(now);
        project.setUpdateTime(now);
        project.setCreateBy(1L); // TODO: 从SecurityContext获取当前用户ID
        project.setUpdateBy(1L); // TODO: 从SecurityContext获取当前用户ID
        
        return this.save(project);
    }

    @Override
    public boolean updateProject(Project project) {
        // 检查项目是否存在
        Project existing = this.getById(project.getProjectId());
        if (existing == null) {
            throw new BizException(BizErrorCode.PROJECT_NOT_FOUND, 
                    "项目不存在: " + project.getProjectId());
        }
        
        // 手动设置更新时间和更新人
        project.setUpdateTime(LocalDateTime.now());
        project.setUpdateBy(1L); // TODO: 从 SecurityContext获取当前用户ID
        
        return this.updateById(project);
    }

    @Override
    public boolean archiveProject(Long projectId) {
        Project project = this.getById(projectId);
        if (project == null) {
            throw new BizException(BizErrorCode.PROJECT_NOT_FOUND, 
                    "项目不存在: " + projectId);
        }
        
        project.setStatus(ProjectStatus.ARCHIVED.getCode()); // 设置为 ARCHIVED
        
        // 手动设置更新时间和更新人
        project.setUpdateTime(LocalDateTime.now());
        project.setUpdateBy(1L); // TODO: 从SecurityContext获取当前用户ID
        
        return this.updateById(project);
    }

    @Override
    public String getProjectStats(Long projectId) {
        // TODO: 实现项目统计逻辑
        // 需要关联查询批次、图像、标签等数据
        return "{}";
    }
}

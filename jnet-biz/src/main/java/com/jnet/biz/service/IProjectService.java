package com.jnet.biz.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.biz.dto.ProjectQueryDTO;
import com.jnet.biz.entity.Project;

/**
 * 项目管理 Service 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
public interface IProjectService extends IService<Project> {

    /**
     * 分页查询项目列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<Project> pageProjects(ProjectQueryDTO query);

    /**
     * 创建项目
     *
     * @param project 项目信息
     * @return 是否成功
     */
    boolean createProject(Project project);

    /**
     * 更新项目
     *
     * @param project 项目信息
     * @return 是否成功
     */
    boolean updateProject(Project project);

    /**
     * 归档项目（软删除）
     *
     * @param projectId 项目ID
     * @return 是否成功
     */
    boolean archiveProject(Long projectId);

    /**
     * 获取项目统计信息
     *
     * @param projectId 项目ID
     * @return 统计信息JSON字符串
     */
    String getProjectStats(Long projectId);
}

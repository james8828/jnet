package com.jnet.biz.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.biz.dto.DatasetTaskQueryDTO;
import com.jnet.biz.dto.DatasetTaskSuccessDTO;
import com.jnet.biz.entity.DatasetBuildTask;

/**
 * 数据集构建任务 Service 接口
 *
 * @author JNet Team
 * @since 2024-05-11
 */
public interface IDatasetBuildTaskService extends IService<DatasetBuildTask> {

    /**
     * 分页查询任务列表
     *
     * @param query 查询条件（包含分页参数）
     * @return 分页结果
     */
    Page<DatasetBuildTask> pageTasks(DatasetTaskQueryDTO query);

    /**
     * 创建数据集构建任务
     *
     * @param task 任务对象
     * @return 任务ID
     */
    Long createTask(DatasetBuildTask task);

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(Long taskId);

    /**
     * 更新任务进度
     *
     * @param taskId 任务ID
     * @param progress 进度（0-100）
     * @param currentStep 当前步骤
     * @param stepDetail 步骤详情（JSON）
     */
    void updateProgress(Long taskId, Float progress, String currentStep, String stepDetail);

    /**
     * 标记任务成功
     *
     * @param successDTO 成功结果 DTO，包含所有成功相关信息
     */
    void markTaskSuccess(DatasetTaskSuccessDTO successDTO);

    /**
     * 标记任务失败
     *
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     * @param errorStack 错误堆栈
     */
    void markTaskFailed(Long taskId, String errorMessage, String errorStack);
}

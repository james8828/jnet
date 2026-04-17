package com.jnet.biz.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.biz.dto.BatchQueryDTO;
import com.jnet.biz.entity.Batch;

import java.util.List;

/**
 * 采集批次 Service 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
public interface IBatchService extends IService<Batch> {

    /**
     * 分页查询批次列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<Batch> pageBatches(BatchQueryDTO query);

    /**
     * 创建批次
     *
     * @param batch 批次信息
     * @return 是否成功
     */
    boolean createBatch(Batch batch);

    /**
     * 获取项目下的所有批次
     *
     * @param projectId 项目ID
     * @return 批次列表
     */
    List<Batch> listByProjectId(Long projectId);
}

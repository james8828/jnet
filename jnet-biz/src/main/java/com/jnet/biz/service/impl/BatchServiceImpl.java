package com.jnet.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.biz.dto.BatchQueryDTO;
import com.jnet.biz.entity.Batch;
import com.jnet.biz.enums.UploadStatus;
import com.jnet.biz.mapper.BatchMapper;
import com.jnet.biz.service.IBatchService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集批次 Service 实现类
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Service
public class BatchServiceImpl extends ServiceImpl<BatchMapper, Batch> implements IBatchService {

    @Override
    public Page<Batch> pageBatches(BatchQueryDTO query) {
        // 验证分页参数
        query.validate();
        
        Page<Batch> page = query.toPage();
        LambdaQueryWrapper<Batch> wrapper = new LambdaQueryWrapper<>();
        
        // 所属项目ID筛选
        if (query.getProjectId() != null) {
            wrapper.eq(Batch::getProjectId, query.getProjectId());
        }
        
        // 批次编号模糊查询
        if (StringUtils.hasText(query.getBatchCode())) {
            wrapper.like(Batch::getBatchCode, query.getBatchCode());
        }
        
        // 批次名称模糊查询
        if (StringUtils.hasText(query.getBatchName())) {
            wrapper.like(Batch::getBatchName, query.getBatchName());
        }
        
        // 扫描仪型号筛选
        if (StringUtils.hasText(query.getScannerModel())) {
            wrapper.eq(Batch::getScannerModel, query.getScannerModel());
        }
        
        // 上传状态筛选
        if (query.getUploadStatus() != null) {
            wrapper.eq(Batch::getUploadStatus, query.getUploadStatus());
        }
        
        // 排序处理
        if (StringUtils.hasText(query.getOrderBy())) {
            if ("asc".equalsIgnoreCase(query.getOrderDirection())) {
                wrapper.orderByAsc(getOrderColumn(query.getOrderBy()));
            } else {
                wrapper.orderByDesc(getOrderColumn(query.getOrderBy()));
            }
        } else {
            wrapper.orderByDesc(Batch::getCreateTime);
        }
        
        return this.page(page, wrapper);
    }

    /**
     * 获取排序字段（防止SQL注入）
     */
    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<Batch, ?> getOrderColumn(String orderBy) {
        return switch (orderBy.toLowerCase()) {
            case "batch_code" -> Batch::getBatchCode;
            case "batch_name" -> Batch::getBatchName;
            case "create_time" -> Batch::getCreateTime;
            default -> Batch::getCreateTime; // 默认按创建时间排序
        };
    }

    @Override
    public boolean createBatch(Batch batch) {
        // 设置默认值
        if (batch.getUploadStatus() == null) {
            batch.setUploadStatus(UploadStatus.PENDING.getCode()); // 默认为 PENDING
        }
        if (batch.getTotalImages() == null) {
            batch.setTotalImages(0);
        }
        
        // 手动设置审计字段
        LocalDateTime now = LocalDateTime.now();
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setCreateBy(1L); // TODO: 从SecurityContext获取当前用户ID
        batch.setUpdateBy(1L); // TODO: 从SecurityContext获取当前用户ID
        
        return this.save(batch);
    }

    @Override
    public List<Batch> listByProjectId(Long projectId) {
        LambdaQueryWrapper<Batch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Batch::getProjectId, projectId);
        wrapper.orderByDesc(Batch::getCreateTime);
        return this.list(wrapper);
    }
}

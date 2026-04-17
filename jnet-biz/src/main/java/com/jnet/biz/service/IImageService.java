package com.jnet.biz.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.biz.dto.BatchSelectImagesDTO;
import com.jnet.biz.dto.ImageQueryDTO;
import com.jnet.biz.entity.Image;

import java.util.List;

/**
 * 图像资产 Service 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
public interface IImageService extends IService<Image> {

    /**
     * 高级检索图像
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<Image> searchImages(ImageQueryDTO query);

    /**
     * 更新图像生命周期状态
     *
     * @param imageId 图像ID
     * @param status  新状态
     * @return 是否成功
     */
    boolean updateLifecycleStatus(Long imageId, String status);

    /**
     * 批量更新标注进度
     *
     * @param imageIds 图像ID列表
     * @param progress 标注进度
     * @return 是否成功
     */
    boolean batchUpdateAnnotationProgress(List<Long> imageIds, Integer progress);

    /**
     * 批量选择切片（移动/复制）
     *
     * @param dto 批量选择参数
     * @return 是否成功
     */
    boolean batchSelectImages(BatchSelectImagesDTO dto);
}

package com.jnet.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.biz.entity.Tag;

import java.util.List;

/**
 * 标签管理 Service 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
public interface ITagService extends IService<Tag> {

    /**
     * 获取标签树形结构
     *
     * @param category 标签分类（可选）
     * @return 树形结构JSON
     */
    String getTagTree(String category);

    /**
     * 批量给图像打标
     *
     * @param imageIds 图像ID列表
     * @param tagIds   标签ID列表
     * @return 是否成功
     */
    boolean batchAssignTags(List<Long> imageIds, List<Long> tagIds);
}

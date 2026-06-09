package com.jnet.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.biz.entity.YoloTrainingTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * YOLO模型训练任务 Mapper 接口
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Mapper
public interface YoloTrainingTaskMapper extends BaseMapper<YoloTrainingTask> {
}

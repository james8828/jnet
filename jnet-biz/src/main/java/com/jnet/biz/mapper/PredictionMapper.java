package com.jnet.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.biz.entity.Prediction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预测结果 Mapper 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Mapper
public interface PredictionMapper extends BaseMapper<Prediction> {
}

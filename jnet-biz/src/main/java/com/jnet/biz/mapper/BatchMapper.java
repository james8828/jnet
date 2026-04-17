package com.jnet.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.biz.entity.Batch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集批次 Mapper 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Mapper
public interface BatchMapper extends BaseMapper<Batch> {
}

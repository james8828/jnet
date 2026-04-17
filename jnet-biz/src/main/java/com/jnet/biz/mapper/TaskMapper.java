package com.jnet.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.biz.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务执行 Mapper 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}

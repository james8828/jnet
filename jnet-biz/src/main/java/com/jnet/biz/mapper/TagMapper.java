package com.jnet.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.biz.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签定义 Mapper 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}

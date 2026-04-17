package com.jnet.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.biz.entity.Model;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型注册 Mapper 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Mapper
public interface ModelMapper extends BaseMapper<Model> {
}

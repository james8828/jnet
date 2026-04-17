package com.jnet.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.biz.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目管理 Mapper 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}

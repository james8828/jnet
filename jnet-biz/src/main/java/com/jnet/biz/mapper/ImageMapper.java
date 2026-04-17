package com.jnet.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.biz.entity.Image;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图像资产 Mapper 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Mapper
public interface ImageMapper extends BaseMapper<Image> {
}

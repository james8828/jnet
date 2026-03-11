package com.jnet.image.vo;

import com.jnet.api.vo.PageQueryVO;
import lombok.Data;

/**
 * @author mugw
 * @version 1.0
 * @description 切片分页查询条件
 * @date 2025/6/25 15:01:38
 */
@Data
public class SlidePageQueryVo extends PageQueryVO {
    /**
     * 无扩展名文件名称
     */
    private String slideName;
    /**
     * 处理状态，1-上传中;2-解析中;3-可用;4-解析失败;5上传失败
     */
    private Integer status;
}

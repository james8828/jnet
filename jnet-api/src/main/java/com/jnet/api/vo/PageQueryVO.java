package com.jnet.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/10/11 10:28:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageQueryVO {
    @Schema(name = "pageNum", description = "当前页")
    private Long pageNum;
    @Schema(name = "pageSize", description = "页大小")
    private Long pageSize;
}

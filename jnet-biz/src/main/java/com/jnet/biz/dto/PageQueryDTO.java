package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基类
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "分页查询基类")
public class PageQueryDTO {

    /**
     * 当前页码（从1开始）
     */
    @Schema(description = "当前页码", example = "1", defaultValue = "1", minimum = "1")
    private Long current = 1L;

    /**
     * 每页数量
     */
    @Schema(description = "每页数量", example = "10", defaultValue = "10", minimum = "1", maximum = "100")
    private Long size = 10L;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "create_time")
    private String orderBy;

    /**
     * 排序方向（asc/desc）
     */
    @Schema(description = "排序方向", example = "desc", allowableValues = {"asc", "desc"})
    private String orderDirection = "desc";

    /**
     * 获取MyBatis Plus分页对象
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
    }

    /**
     * 获取偏移量（用于LIMIT OFFSET）
     */
    public Long getOffset() {
        return (current - 1) * size;
    }

    /**
     * 验证分页参数
     */
    public void validate() {
        if (current == null || current < 1) {
            current = 1L;
        }
        if (size == null || size < 1) {
            size = 10L;
        }
        if (size > 100) {
            size = 100L; // 限制最大每页数量
        }
    }
}

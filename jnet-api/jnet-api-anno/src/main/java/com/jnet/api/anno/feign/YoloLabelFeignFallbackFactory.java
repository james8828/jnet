package com.jnet.api.anno.feign;

import com.jnet.api.anno.dto.YoloLabelData;
import com.jnet.api.anno.dto.YoloLabelQueryRequest;
import com.jnet.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * YOLO标注查询 Feign 客户端降级工厂
 * 当 anno 服务不可用时，提供降级处理
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Slf4j
@Component
public class YoloLabelFeignFallbackFactory implements FallbackFactory<YoloLabelFeignClient> {
    
    @Override
    public YoloLabelFeignClient create(Throwable cause) {
        return new YoloLabelFeignClient() {
            
            @Override
            public Result<List<YoloLabelData>> queryYoloLabels(YoloLabelQueryRequest request) {
                log.error("调用 anno 服务查询YOLO标注失败，触发降级处理。请求参数: {}", request, cause);
                
                // 降级策略：返回空列表
                return Result.success(Collections.emptyList());
            }
            
            @Override
            public Result<List<YoloLabelData>> queryByImageIds(List<Long> imageIds) {
                log.error("调用 anno 服务按图像ID查询YOLO标注失败，触发降级处理。图像IDs: {}", imageIds, cause);
                
                // 降级策略：返回空列表
                return Result.success(Collections.emptyList());
            }
        };
    }
}

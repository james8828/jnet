package com.jnet.api.anno.feign;

import com.jnet.api.anno.dto.YoloLabelData;
import com.jnet.api.anno.dto.YoloLabelQueryRequest;
import com.jnet.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * YOLO标注查询 Feign 客户端
 * 用于其他微服务调用 anno 服务的 YOLO 标注查询接口
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@FeignClient(
    name = "jnet-anno",
    path = "/anno/api/v1/feign/yolo-labels",
    contextId = "yoloLabelFeignClient",
    fallbackFactory = YoloLabelFeignFallbackFactory.class
)
public interface YoloLabelFeignClient {
    
    /**
     * 查询YOLO格式标注数据
     * 
     * @param request 查询请求，包含项目、批次、标签、图像ID等筛选条件
     * @return YOLO格式标注数据列表
     */
    @PostMapping("/query")
    Result<List<YoloLabelData>> queryYoloLabels(@RequestBody YoloLabelQueryRequest request);
    
    /**
     * 根据图像ID集合查询YOLO标注（快捷接口）
     * 
     * @param imageIds 图像ID集合
     * @return YOLO格式标注数据列表
     */
    @PostMapping("/by-images")
    Result<List<YoloLabelData>> queryByImageIds(@RequestBody List<Long> imageIds);
}

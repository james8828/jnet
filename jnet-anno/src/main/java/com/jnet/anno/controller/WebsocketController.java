package com.jnet.anno.controller;

import com.jnet.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author mugw
 * @version 1.0
 * @description 测量标注表
 * @date 2025/5/21 14:33:43
 */
@RestController
@RequestMapping("/api/v1/websocket")
public class WebsocketController {

    @Value("${netty.port}")
    private Integer port;

    @Operation(summary = "websocket接口")
    @GetMapping("/getWebsocketPort")
    public Result<String> getWebsocketPort() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return Result.error();
        }
        HttpServletRequest request = requestAttributes.getRequest();
        String localAdd = request.getLocalAddr();
        return Result.success("ws://" + localAdd + ":" + port + "/");
    }

}

package com.jnet.image.slide.controller;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/8/7 15:16:07
 */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/*@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/forward")
    public ResponseEntity<String> forwardRequest() {
        // 目标服务URL
        String targetUrl = "http://target-service.com/api/data";

        // 转发请求到目标服务
        ResponseEntity<String> response = restTemplate.getForEntity(targetUrl, String.class);

        return response;
    }
}*/


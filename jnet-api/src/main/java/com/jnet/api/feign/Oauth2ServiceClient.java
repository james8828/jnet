package com.jnet.api.feign;

import com.jnet.api.R;
import com.jnet.api.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/6/16 20:21:13
 */
@FeignClient(value = "jnet-oauth2-service",path = "/", configuration = FeignConfig.class, fallbackFactory = Oauth2ServiceClientFallbackFactory.class, dismiss404 = true)
public interface Oauth2ServiceClient {
    @GetMapping("/auth/jwt")
    R getJwt() throws Exception;
}

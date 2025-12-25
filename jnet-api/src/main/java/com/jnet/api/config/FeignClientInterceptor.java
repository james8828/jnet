package com.jnet.api.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author mugw
 * @version 1.0
 * @since 2025/6/13 16:55:26
 */
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 获取原始请求中的 Authorization header
            String authorization = request.getHeader("Authorization");

            if (authorization != null && !authorization.isEmpty()) {
                // 将 token 添加到 Feign 请求头中
                template.header("Authorization", authorization);
            }
        }
    }
}

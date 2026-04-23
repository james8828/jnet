package com.jnet.biz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置
 * 解决前端调用后端API时的跨域问题，特别是OPTIONS预检请求
 *
 * @author JNet Team
 * @since 2024-04-17
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        log.info("初始化CORS配置...");
        
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许所有域名访问（开发环境）
        config.addAllowedOriginPattern("*");
        
        // 允许所有请求头
        config.addAllowedHeader("*");
        
        // 允许所有请求方法（GET, POST, PUT, DELETE, OPTIONS等）
        config.addAllowedMethod("*");
        
        // 允许携带认证信息（cookies、authorization headers等）
        config.setAllowCredentials(true);
        
        // 预检请求的有效期（秒），在此期间不需要再次发送预检请求
        config.setMaxAge(3600L);
        
        // 暴露的响应头
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("X-Requested-With");
        config.addExposedHeader("accept");
        config.addExposedHeader("Origin");
        config.addExposedHeader("Access-Control-Request-Method");
        config.addExposedHeader("Access-Control-Request-Headers");
        
        // 注册CORS配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", config);
        
        log.info("CORS配置初始化完成: 允许所有来源、所有方法、所有请求头");
        
        return new CorsFilter(source);
    }
}

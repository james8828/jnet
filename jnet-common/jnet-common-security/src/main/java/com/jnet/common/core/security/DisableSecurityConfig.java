package com.jnet.common.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/25 14:59:25
 */
public class DisableSecurityConfig {
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.disable())
                .headers((headers) -> headers.frameOptions(frameOptionsConfig -> frameOptionsConfig.sameOrigin()))
                .authorizeHttpRequests((authorize) -> {
                    authorize.anyRequest().permitAll();
                });
        return http.build();
    }
}

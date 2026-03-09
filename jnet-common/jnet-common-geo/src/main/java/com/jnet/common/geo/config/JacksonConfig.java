package com.jnet.common.geo.config;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jts geo序列化配置
 *
 * @author mu
 * @version 1.0
 * @since 2026/3/9
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }
}


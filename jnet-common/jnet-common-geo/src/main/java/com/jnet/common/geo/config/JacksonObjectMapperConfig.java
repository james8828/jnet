package com.jnet.common.geo.config;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * objectMapper 配置
 *
 * @author mu
 * @version 1.0
 * @since 2026/3/9
 */
@Configuration
public class JacksonObjectMapperConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(JtsModule jtsModule) {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 JTS 模块
        mapper.registerModule(new JtsModule());

        // 其他配置
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}


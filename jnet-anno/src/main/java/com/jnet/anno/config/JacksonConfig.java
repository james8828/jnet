package com.jnet.anno.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnet.anno.geojson.JtsModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

import java.util.List;

/**
 * JTS GeoJSON 序列化/反序列化配置
 */
@Configuration
public class JacksonConfig implements WebMvcConfigurer {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 关键！配置 HttpMessageConverter 使用自定义的 ObjectMapper
     * 这样@RequestBody 才能正确反序列化 Geometry
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 移除默认的 MappingJackson2HttpMessageConverter
        converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);

        // 添加自定义的 Converter
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        objectMapper.registerModule(new JtsModule());
        jacksonConverter.setObjectMapper(objectMapper);
        converters.add(0, jacksonConverter);

        System.out.println("✅ Custom HttpMessageConverter configured with JTS Module!");
        System.out.println("✅ ObjectMapper modules: " + objectMapper.getRegisteredModuleIds());
    }
}

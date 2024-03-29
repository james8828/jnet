package com.jnet.anno.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.jnet.anno.common.datatype.jts.JtsModule;
import com.jnet.anno.utils.JsonUtil;
import org.locationtech.spatial4j.io.jackson.ShapesAsGeoJSONModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jackson配置
 */
@Configuration
public class JacksonConfig {


    /*
     * Jackson Afterburner module to speed up serialization/deserialization.
     */
    /*@Bean
    public AfterburnerModule afterburnerModule() {
        AfterburnerModule module = new AfterburnerModule();
        // make Afterburner generate bytecode only for public getters/setter and fields
        // without this, Java 9+ complains of "Illegal reflective access"
        module.setUseValueClassLoader(false);
        return module;
    }*/

    @Bean
    public ParameterNamesModule parameterNamesModule() {
        return new ParameterNamesModule();
    }

    @Bean
    public Jdk8Module jdk8Module() {
        return new Jdk8Module();
    }

    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    public ShapesAsGeoJSONModule shapesAsGeoJSONModule(){
        return new ShapesAsGeoJSONModule();
    }

    @Bean
    public JtsModule jtsModule(){
        return new JtsModule();
    }

    @Bean
    public JsonUtil jsonUtil(ObjectMapper objectMapper) {
        //只是把objectmapper注入
        JsonUtil.objectMapper = objectMapper;
        return new JsonUtil();
    }

}

package com.jnet.common.geo.config;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.jnet.common.geo.jackson.JtsModule;
import com.jnet.common.geo.jackson.JtsModule3D;
import com.jnet.common.geo.mybatis.handler.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/5/21 10:19:32
 */
@Configuration
public class JTSAutoConfiguration {

    @Bean
    public ObjectMapper customObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JtsModule3D jtsModule3D = new JtsModule3D();
        JtsModule jtsModule = new JtsModule();
        mapper.registerModule(jtsModule);
        mapper.registerModule(jtsModule3D);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Date.class, new DateSerializer());
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
        javaTimeModule.addDeserializer(Date.class, new DateDeserializers.DateDeserializer());
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
        mapper.registerModule(javaTimeModule);
        return mapper;
    }


    @Bean
    public JtsLinearRingTypeHandler linearRingTypeHandler() {
        return new JtsLinearRingTypeHandler();
    }

    @Bean
    public JtsLineStringTypeHandler lineStringTypeHandler(){
        return new JtsLineStringTypeHandler();
    }

    @Bean
    public JtsMultiLineStringTypeHandler multiLineStringTypeHandler() {
        return new JtsMultiLineStringTypeHandler();
    }

    @Bean
    public JtsMultiPointTypeHandler multiPointTypeHandler() {
        return new JtsMultiPointTypeHandler();
    }

    @Bean
    public JtsMultiPolygonTypeHandler multiPolygonTypeHandler() {
        return new JtsMultiPolygonTypeHandler();
    }

    @Bean
    public JtsPointTypeHandler pointTypeHandler() {
        return new JtsPointTypeHandler();
    }

    @Bean
    public JtsPolygonTypeHandler polygonTypeHandler() {
        return new JtsPolygonTypeHandler();
    }

    @Bean
    public JtsGeometryTypeHandler geometryTypeHandler() {
        return new JtsGeometryTypeHandler();
    }
}


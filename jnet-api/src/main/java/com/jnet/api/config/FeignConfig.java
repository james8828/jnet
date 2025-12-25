package com.jnet.api.config;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import feign.Feign;
import feign.Logger;
import lombok.SneakyThrows;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/30 15:18:23
 */
@Configuration
@ConditionalOnClass(Feign.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class FeignConfig {

    @Bean
    public Logger.Level feignLevel(){
        return Logger.Level.FULL;
    }

//    @Bean
//    public Decoder feignDecoder() {
//        return new ResponseEntityDecoder(new SpringDecoder(() -> {
//            return new HttpMessageConverters(mappingJackson2HttpMessageConverter());
//        }));
//    }

    @Bean
    @ConditionalOnMissingBean
    public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters, MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
        List<HttpMessageConverter<?>> list = converters.orderedStream().collect(Collectors.toList());
        list.add(mappingJackson2HttpMessageConverter);
        return new HttpMessageConverters(list);
    }

    @Bean
    @ConditionalOnMissingBean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Date.class, new DateSerializer());
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
        javaTimeModule.addDeserializer(Date.class, new DateDeserializer());
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
        om.registerModule(javaTimeModule);
        //om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    public static class DateSerializer extends JsonSerializer<Date> {
        @Override
        public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(DateUtil.format(date, DatePattern.NORM_DATETIME_FORMAT));
        }
    }

    public static class DateDeserializer extends JsonDeserializer<Date> {
        @SneakyThrows
        @Override
        public Date deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {
            return DateUtil.parseDateTime(p.getValueAsString());
        }
    }

}

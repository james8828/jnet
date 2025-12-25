package com.jnet.system;

import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/19 11:08:28
 */
@EnableFeignClients("com.jnet.api.feign")
@EnableDiscoveryClient
@SpringBootApplication()
@MapperScan(basePackages = "com.jnet.system.mapper")
public class SystemService {

    public static void main(String[] args) {
        SpringApplication.run(SystemService.class, args);
    }

    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        return new PaginationInnerInterceptor();
    }
}

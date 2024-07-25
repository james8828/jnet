package com.jnet.system;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/19 11:08:28
 */
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan(basePackages = "com.jnet.system.mapper")
public class SystemApp {

    public static void main(String[] args) {
        SpringApplication.run(SystemApp.class, args);
    }

    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        return new PaginationInnerInterceptor();
    }
}

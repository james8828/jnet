package com.jnet.anno;

import cn.hutool.extra.spring.EnableSpringUtil;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MapperScan(basePackages = "com.jnet.anno.mapper")
@SpringBootApplication
@EnableDiscoveryClient
@EnableTransactionManagement
@EnableScheduling
@EnableSpringUtil
public class AnnoService
{
    public static void main( String[] args )
    {
        SpringApplication.run(AnnoService.class);
    }

    @Bean
    public MybatisPlusInterceptor paginationInterceptor() {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return mybatisPlusInterceptor;
    }
}



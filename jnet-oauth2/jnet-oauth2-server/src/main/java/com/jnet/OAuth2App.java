package com.jnet;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.jnet.api.config.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

/**
 * @author mugw
 * @version 1.0
 * @description 认证授权服务
 * @date 2024/7/8 15:31:30
 */
@EnableFeignClients
@EnableDiscoveryClient
@Import(FeignConfig.class)
@SpringBootApplication(exclude = {MybatisPlusAutoConfiguration.class, DataSourceAutoConfiguration.class})
public class OAuth2App
{
    public static void main( String[] args )
    {
        SpringApplication.run(OAuth2App.class, args);
    }
}

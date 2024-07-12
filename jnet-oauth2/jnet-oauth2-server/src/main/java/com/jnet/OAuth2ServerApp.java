package com.jnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author mugw
 * @version 1.0
 * @description 认证授权服务
 * @date 2024/7/8 15:31:30
 */
@SpringBootApplication
public class OAuth2ServerApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(OAuth2ServerApp.class, args);
    }
}

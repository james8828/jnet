package com.jnet.anno;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = "com.jnet.anno.biz.mapper")
@SpringBootApplication
public class AnnoApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(AnnoApp.class);
    }
}

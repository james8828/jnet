package com.jnet.biz.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 配置类
 * 
 * @author JNet Team
 * @since 2024-04-16
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:jnet-biz}")
    private String applicationName;

    @Value("${server.port:9200}")
    private Integer serverPort;

    /**
     * 配置OpenAPI文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("病理AI数据池管理系统 API文档")
                        .description("""
                                ## 系统简介
                                病理AI数据池管理系统提供项目、批次、图像资产、标签的全生命周期管理。
                                
                                ## 核心功能
                                - **项目管理**: 创建、查询、更新、归档病理分析项目
                                - **批次管理**: 管理图像采集批次，跟踪上传状态
                                - **图像管理**: 高级检索、生命周期管理、标注进度跟踪
                                - **标签管理**: 树形标签体系、批量打标
                                
                                ## 技术栈
                                - Spring Boot 3.2.7
                                - MyBatis Plus 3.5.5
                                - PostgreSQL + PostGIS
                                - Redis + Redisson
                                
                                ## 认证方式
                                所有接口需要在Header中携带JWT Token：
                                ```
                                Authorization: Bearer {your_token}
                                ```
                                
                                ## 响应格式
                                所有接口统一返回Result格式：
                                ```json
                                {
                                  "code": 10000,
                                  "msg": "操作成功",
                                  "data": {}
                                }
                                ```
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("JNet Team")
                                .email("support@jnet.com")
                                .url("https://jnet.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("本地开发环境"),
                        new Server()
                                .url("http://dev.jnet.com")
                                .description("开发环境"),
                        new Server()
                                .url("http://test.jnet.com")
                                .description("测试环境"),
                        new Server()
                                .url("http://prod.jnet.com")
                                .description("生产环境")
                ));
    }
}

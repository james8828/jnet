package com.jnet.anno.config;

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
 * OpenAPI 配置类
 *
 * @author JNet Team
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:9005}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        String serverUrl = "http://localhost:" + serverPort + (contextPath != null ? contextPath : "");
        
        return new OpenAPI()
                .openapi("3.0.1")
                .info(new Info()
                        .title("病理AI标注系统 API")
                        .version("1.0.0")
                        .description("病理AI数据池标注管理系统的 RESTful API 文档")
                        .contact(new Contact()
                                .name("JNet Team")
                                .email("support@jnet.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url(serverUrl).description("本地开发环境")
                ));
    }
}

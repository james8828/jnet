package com.jnet.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/3 09:33:39
 */
@Slf4j
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("JNET")
                        .description("API文档")
                        .version("v1")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("外部文档")
                        .url("https://springshop.wiki.github.org/docs"));
    }

}

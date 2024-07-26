package com.jnet.gateway.resource.server;

import com.jnet.common.core.security.JwtConfiguration;
import com.jnet.common.core.security.SecurityComponentConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import static org.springframework.security.config.Customizer.withDefaults;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/25 15:05:03
 */
@Configuration
@EnableWebFluxSecurity
@Import({SecurityComponentConfig.class, JwtConfiguration.class})
public class ResourceServerConfig {
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, PermissionReactiveAuthorizationManager customAuthorizationManager) {
        http
                .authorizeExchange((authorize) -> authorize.anyExchange().access(customAuthorizationManager))
                .oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
                //.oauth2ResourceServer((oauth2) -> oauth2.jwt(jwtConfigurer -> jwtConfigurer.jwkSetUri("http://localhost:9000/rsa/publicKey")));
        return http.build();
    }

    /**
     * 提供基于RSA公钥的响应式JWT解码器。
     *
     * @param keyPair RSA密钥对，仅使用公钥进行JWT解码。
     * @return 响应式JWT解码器，用于异步验证和解析JWT。
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(KeyPair keyPair) {
        return NimbusReactiveJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
    }

}

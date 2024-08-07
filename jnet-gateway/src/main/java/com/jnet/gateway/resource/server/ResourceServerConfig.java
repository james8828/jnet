package com.jnet.gateway.resource.server;

import com.jnet.common.core.security.JwtConfiguration;
import com.jnet.common.core.security.SecurityComponentConfig;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${whitelists}")
    private String[] whitelists;

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, PermissionReactiveAuthorizationManager customAuthorizationManager) {
        /**
         * CsrfWebFilter 是 Spring Security 5 中引入的一个组件，用于处理基于 WebFlux 的应用中的跨站请求伪造（Cross-Site Request Forgery, CSRF）保护。它是在非阻塞的 Web 应用程序中实现 CSRF 保护的关键组件之一。
         * CsrfWebFilter 的作用
         * CsrfWebFilter 主要负责以下任务：
         * 生成 CSRF 令牌:
         * 当客户端首次访问应用程序时，CsrfWebFilter 会生成一个唯一的 CSRF 令牌，并将其存储在客户端（通常是通过 cookie）。
         * 验证 CSRF 令牌:
         * 当客户端发送 POST、PUT、DELETE 等修改状态的请求时，CsrfWebFilter 会验证请求中的 CSRF 令牌是否有效。
         * 如果请求中没有包含正确的 CSRF 令牌或者令牌无效，则请求会被拒绝
         */
        http.csrf(csrf -> csrf.disable())
                .authorizeExchange((authorize) -> authorize.pathMatchers(whitelists).permitAll() // 公开路径
                        .anyExchange().access(customAuthorizationManager))
                //.authorizeExchange((authorize) -> authorize.anyExchange().permitAll())
                .oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
                /*从oauth2-server服务获取公钥
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(jwtConfigurer -> jwtConfigurer.jwkSetUri("http://localhost:9000/rsa/publicKey")));*/
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

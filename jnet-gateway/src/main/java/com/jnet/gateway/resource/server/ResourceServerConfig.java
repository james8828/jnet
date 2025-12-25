package com.jnet.gateway.resource.server;

import cn.hutool.json.JSONUtil;
import com.jnet.api.R;
import com.jnet.common.core.security.JwtConfiguration;
import com.jnet.common.core.security.SecurityComponentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.config.Customizer.withDefaults;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/25 15:05:03
 */
@Slf4j
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
        //todo 调用认证服务自省endpoint，获取当前用户信息
        http.csrf(csrf -> csrf.disable())
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .headers(hSpec -> hSpec.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
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

    class CustomAuthenticationFailureHandler implements ServerAuthenticationFailureHandler {

        @Override
        public Mono<Void> onAuthenticationFailure(WebFilterExchange exchange, AuthenticationException ex) {
            // 获取当前请求的响应对象
            ServerHttpResponse response = exchange.getExchange().getResponse();

            // 设置 HTTP 响应状态码为 401 Unauthorized
            response.setStatusCode(OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            // 创建一个 JSON 对象来表示失败的原因
            R errorResponse = R.fail(HttpStatus.UNAUTHORIZED.value(),ex.getMessage());

            // 设置响应的内容类型为 JSON
            DataBuffer buffer = response.bufferFactory().wrap(JSONUtil.toJsonPrettyStr(errorResponse).getBytes(StandardCharsets.UTF_8));

            // 写入响应体
            return response.writeWith(Mono.just(buffer));
        }
    }

}

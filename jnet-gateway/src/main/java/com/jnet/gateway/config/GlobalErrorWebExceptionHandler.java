package com.jnet.gateway.config;

import cn.hutool.json.JSONUtil;
import com.jnet.api.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.ZonedDateTime;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/9/19 17:31:17
 */

@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String message = ex.getMessage();
        long timestamp = ZonedDateTime.now().toEpochSecond();
        if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST.value();
        } else if (ex instanceof BadJwtException) {
            status = HttpStatus.UNAUTHORIZED.value();
        } else if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN.value();
        }
        // 构建错误响应对象
        R errorResponse = R.fail(status,message);
        // 设置响应状态码
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(status));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (log.isDebugEnabled()){
            ex.printStackTrace();
        }
        log.error("GlobalErrorWebException:[{}]",message);
        // 返回响应体
        return response.writeWith(Mono.just(response.bufferFactory().wrap(JSONUtil.toJsonPrettyStr(errorResponse).getBytes())));
    }

}


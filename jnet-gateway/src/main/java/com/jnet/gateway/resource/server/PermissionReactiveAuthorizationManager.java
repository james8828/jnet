package com.jnet.gateway.resource.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author mugw
 * @version 1.0
 * @description 响应式权限管理器 todo 后续改进
 * @date 2024/7/8 16:21:57
 */
@Slf4j
@Component
public class PermissionReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext>
{
    private static final AuthorizationDecision DENY = new AuthorizationDecision(false);
    private static final AuthorizationDecision ACCEPT = new AuthorizationDecision(true);

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext authorizationContext) {
        Map<String, String[]> urlToRoles = new HashMap<>();
        PathPatternParser parser = new PathPatternParser();
        List<PathPattern> patterns = urlToRoles.keySet().stream().map(parser::parse).collect(Collectors.toList());
        ServerHttpRequest serverHttpRequest = authorizationContext.getExchange().getRequest();
        for (PathPattern pattern : patterns) {
            if (pattern.matches(PathContainer.parsePath(serverHttpRequest.getURI().getPath()))) {
                String[] roles = urlToRoles.get(pattern.getPatternString());
                Set<String> roleSet = new HashSet<>(Arrays.asList(roles)); // 将String数组转换为Set
                return authentication.flatMap(auth -> {
                    Set<String> authorities = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    // 找到两个集合的交集
                    authorities.retainAll(roleSet);
                    return Mono.just(ACCEPT);
                });
            }
        }
        return Mono.just(DENY);
    }

}

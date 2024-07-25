/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jnet.oauth2.server.authorizationManager;


import com.jnet.oauth2.server.config.DynamicSecurityConfig;
import jakarta.annotation.Resource;
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
public abstract class PermissionReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext>
{
    private static final AuthorizationDecision DENY = new AuthorizationDecision(false);
    private static final AuthorizationDecision ACCEPT = new AuthorizationDecision(true);
    @Resource
    private DynamicSecurityConfig dynamicSecurityConfig;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext authorizationContext) {
        Map<String, String[]> urlToRoles = this.dynamicSecurityConfig.getUrlToRoles();
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

    /*@Override
    public List<SysMenu> findMenuByRoleCodes(String roleCodes) {
        Future<List<SysMenu>> futureResult = asynMenuService.findByRoleCodes(roleCodes);
        try {
            return futureResult.get();
        } catch (Exception e) {
            log.error("asynMenuService.findMenuByRoleCodes-error", e);
        }
        return Collections.emptyList();
    }*/


}

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

package com.jnet.gateway.resource.server;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/8 17:07:51
 */
@Slf4j
@Component
public class PermissionAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private static final AuthorizationDecision DENY = new AuthorizationDecision(false);
    private static final AuthorizationDecision ACCEPT = new AuthorizationDecision(true);

    @Resource
    private UserDetailsService userDetailsService;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext requestAuthorizationContext) {
        HttpServletRequest request = requestAuthorizationContext.getRequest();
        Map<String, String[]> urlToRoles = new HashMap<>();
        PathPatternParser parser = new PathPatternParser();
        List<PathPattern> patterns = urlToRoles.keySet().stream().map(parser::parse).collect(Collectors.toList());
        for (PathPattern pattern : patterns) {
            if (pattern.matches(PathContainer.parsePath(request.getRequestURI()))) {
                String[] roles = urlToRoles.get(pattern.getPatternString());
                Set<String> roleSet = new HashSet<>(Arrays.asList(roles)); // 将String数组转换为Set
                Object object = authentication.get().getPrincipal();
                Map<String, Object> claims = ((Jwt) object).getClaims();
                String user = (String) claims.get("sub");
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(user);
                Set<String> authorities = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());
                if (roleSet.retainAll(authorities)) {
                    return ACCEPT;
                }
            }
        }
        return DENY;
    }

    @Override
    public void verify(Supplier<Authentication> authentication, RequestAuthorizationContext requestAuthorizationContext) {
        AuthorizationDecision decision = check(authentication, requestAuthorizationContext);
        if (decision != null && !decision.isGranted()) {
            throw new AccessDeniedException("Access Denied");
        }
    }
}


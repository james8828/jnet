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

package com.jnet.oauth2.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/8 15:31:30
 */
@Component
@ConfigurationProperties(prefix = "security.urls")
public class DynamicSecurityConfig {

    private Map<String, String[]> urlToRoles = new HashMap<>();

    public DynamicSecurityConfig() {
        this.urlToRoles.put("/auth/**",new String[]{"ROLE_GUEST", "ROLE_USER", "ROLE_ADMIN"});
    }

    public Map<String, String[]> getUrlToRoles() {
        return this.urlToRoles;
    }

    public void setUrlToRoles(Map<String, String[]> urlToRoles) {
        this.urlToRoles = urlToRoles;
    }

    public DefaultFilterInvocationSecurityMetadataSource metadataSource() {
        LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> requestMap = new LinkedHashMap<>();
        this.urlToRoles.forEach((path, roles) -> {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(path);
            List<ConfigAttribute> securityConfigs = new ArrayList<>();
            Arrays.stream(roles).forEach(role -> {
                securityConfigs.add(new SecurityConfig(role));
            });
            requestMap.put(matcher, securityConfigs);
        });
        return new DefaultFilterInvocationSecurityMetadataSource(requestMap);
    }

}

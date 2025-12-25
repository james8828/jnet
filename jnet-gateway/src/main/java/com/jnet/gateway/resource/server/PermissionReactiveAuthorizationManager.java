package com.jnet.gateway.resource.server;

import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.Role;
import com.jnet.api.system.domain.User;
import com.jnet.api.feign.SystemServiceClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


/**
 * @author mugw
 * @version 1.0
 * @description 响应式权限管理器 todo 后续改进
 * @date 2024/7/8 16:21:57
 */
@Slf4j
@Component
public class PermissionReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {
    // 可以写死用于测试，后续建议通过 @Value 或配置中心注入
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/image-service/slide/getThumbnailImage/**"
    );

    private static final AuthorizationDecision DENY = new AuthorizationDecision(false);
    private static final AuthorizationDecision ACCEPT = new AuthorizationDecision(true);
    @Resource
    private SystemServiceClient systemService;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext authorizationContext) {
        return authentication.map(auth -> {
            log.info("用户：{}", auth.getName());
            log.info("权限：{}", auth.getAuthorities());
            log.info("请求：{}", authorizationContext.getExchange().getRequest().getURI().getPath());
            log.info("请求：{}", authorizationContext.getExchange().getRequest().getMethod());
            log.info("请求：{}", authorizationContext.getExchange().getRequest().getHeaders());
            log.info("请求：{}", authorizationContext.getExchange().getRequest().getQueryParams());
            log.info("请求：{}", authorizationContext.getExchange().getRequest().getCookies());
            log.info("请求：{}", authorizationContext.getExchange().getRequest().getRemoteAddress());
            log.info("请求：{}", authorizationContext.getExchange().getRequest().getId());
            log.info("请求：{}", authorizationContext.getExchange().getRequest().getHeaders().get("Authorization"));
            String path = authorizationContext.getExchange().getRequest().getURI().getPath();
            log.info("请求路径：{}", path);

            // 白名单匹配逻辑
            PathPatternParser parser = new PathPatternParser();
            List<PathPattern> whitePatterns = WHITE_LIST.stream()
                    .map(parser::parse)
                    .collect(Collectors.toList());

            for (PathPattern pattern : whitePatterns) {
                if (pattern.matches(PathContainer.parsePath(path))) {
                    log.info("路径 {} 匹配白名单，跳过权限检查", path);
                    return ACCEPT; // 直接放行
                }
            }
            try {
                CompletableFuture<User> userCompletableFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return systemService.loadUserByUsername(auth.getName());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                User user = userCompletableFuture.get();
                if (user != null) {
                    Set<Role> roles = user.getRoles();
                    if (roles != null && !roles.isEmpty()) {
                        List<Long> roleIds = roles.stream().map(Role::getRoleId).collect(Collectors.toList());
                        try {
                            CompletableFuture<List<Menu>> menuCompletableFuture = CompletableFuture.supplyAsync(() -> {
                                try {
                                    return systemService.queryMenuByRoleId(roleIds).getData();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            List<Menu> menus = menuCompletableFuture.get();
                            List<PathPattern> patterns = menus.stream().map(Menu::getPath).map(parser::parse).collect(Collectors.toList());
                            for (PathPattern pattern : patterns) {
                                if (pattern.matches(PathContainer.parsePath(authorizationContext.getExchange().getRequest().getURI().getPath()))) {
                                    return ACCEPT;
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            return DENY;
        });
    }

}

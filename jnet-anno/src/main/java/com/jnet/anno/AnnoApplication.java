package com.jnet.anno;

import cn.hutool.extra.spring.EnableSpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@EnableJpaRepositories
@SpringBootApplication
@EnableDiscoveryClient
@EnableTransactionManagement
@EnableScheduling
@EnableSpringUtil
public class AnnoApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(AnnoApplication.class, args);
    }

        @EventListener(ApplicationReadyEvent.class)
    public void printApiUrls(ApplicationReadyEvent event) {
        var env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("server.port", "9005");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String appName = env.getProperty("spring.application.name", "jnet-anno-service");
        
        // 确保 contextPath 以 / 开头且不以 / 结尾
        if (contextPath != null && !contextPath.isEmpty()) {
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
        } else {
            contextPath = "";
        }

        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();

            log.info("\n==========================================================");
            log.info("应用 '{}' 启动成功!", appName);
            log.info("==========================================================");
            log.info("本地访问地址:");
            log.info("  - Swagger UI:      http://localhost:{}{}/swagger-ui.html", port, contextPath);
            log.info("  - Knife4j UI:      http://localhost:{}{}/doc.html", port, contextPath);
            log.info("  - API Docs:        http://localhost:{}{}/v3/api-docs", port, contextPath);
            log.info("  - Actuator:        http://localhost:{}{}/actuator", port, contextPath);
            log.info("外部访问地址:");
            log.info("  - Swagger UI:      http://{}:{}{}/swagger-ui.html", hostAddress, port, contextPath);
            log.info("  - Knife4j UI:      http://{}:{}{}/doc.html", hostAddress, port, contextPath);
            log.info("  - API Docs:        http://{}:{}{}/v3/api-docs", hostAddress, port, contextPath);
            log.info("  - Actuator:        http://{}:{}{}/actuator", hostAddress, port, contextPath);
            log.info("Netty WebSocket 端口: 7777");
            log.info("==========================================================\n");
        } catch (UnknownHostException e) {
            log.warn("无法获取主机地址，仅显示本地访问链接");
            log.info("\n==========================================================");
            log.info("应用 '{}' 启动成功!", appName);
            log.info("==========================================================");
            log.info("访问地址:");
            log.info("  - Swagger UI:      http://localhost:{}{}/swagger-ui.html", port, contextPath);
            log.info("  - Knife4j UI:      http://localhost:{}{}/doc.html", port, contextPath);
            log.info("  - API Docs:        http://localhost:{}{}/v3/api-docs", port, contextPath);
            log.info("  - Actuator:        http://localhost:{}{}/actuator", port, contextPath);
            log.info("Netty WebSocket 端口: 7777");
            log.info("==========================================================\n");
        }
    }


}

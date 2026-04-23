package com.jnet.biz;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 病理AI数据池管理服务启动类
 * 
 * 功能范围：
 * - 项目管理（biz_project）
 * - 批次管理（biz_batch）
 * - 图像资产管理（biz_image）
 * - 标签体系管理（biz_tag, biz_image_tag_rel）
 * - 任务执行管理（biz_task）
 * - 模型注册管理（biz_model）
 * - 预测结果管理（biz_prediction）
 * 
 * 注意：矢量标注功能由 jnet-anno 模块负责
 *
 * @author JNet Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication(exclude = {
    SecurityFilterAutoConfiguration.class  // 排除 Spring Security 自动配置，避免403
})
@EnableDiscoveryClient
@MapperScan("com.jnet.biz.mapper")
@EnableScheduling // 启用定时任务
public class BizApplication {

    public static void main(String[] args) throws UnknownHostException {
        SpringApplication app = new SpringApplication(BizApplication.class);
        Environment env = app.run(args).getEnvironment();
        
        String protocol = "http";
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "9203");
        String contextPath = env.getProperty("server.servlet.context-path", "/biz");
        
        String localUrl = protocol + "://localhost:" + port + contextPath;
        String externalUrl = protocol + "://" + hostAddress + ":" + port + contextPath;
        
        log.info("\n----------------------------------------------------------\n\t" +
                "Application '{}' is running! Access URLs:\n\t" +
                "Local: \t\t{}\n\t" +
                "External: \t{}\n\t" +
                "Knife4j UI: \t{}/doc.html\n\t" +
                "Swagger UI: \t{}/swagger-ui.html\n\t" +
                "API Docs: \t{}/v3/api-docs\n\t" +
                "Actuator: \t{}/actuator\n\t" +
                "Health Check: \t{}/actuator/health\n" +
                "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                localUrl,
                externalUrl,
                localUrl,
                localUrl,
                localUrl,
                localUrl,
                localUrl);
        
        System.out.println("========================================");
        System.out.println("  Business Service Started Successfully!");
        System.out.println("========================================");
    }


}

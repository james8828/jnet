package com.jnet.api.anno.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Feign 客户端熔断器配置
 * 配置 Resilience4j 的熔断器和超时策略
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Configuration
public class FeignCircuitBreakerConfig {
    
    /**
     * 自定义 YOLO 标注查询服务的熔断器配置
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> yoloLabelCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> {
            // 创建熔断器配置
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                // 滑动窗口类型：基于计数
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                // 滑动窗口大小：10次请求
                .slidingWindowSize(10)
                // 最小调用次数：5次（达到此次数后才开始计算失败率）
                .minimumNumberOfCalls(5)
                // 失败率阈值：50%（失败率超过50%时打开熔断器）
                .failureRateThreshold(50)
                // 慢调用阈值：3秒（超过3秒视为慢调用）
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                // 慢调用率阈值：80%（慢调用率超过80%时打开熔断器）
                .slowCallRateThreshold(80)
                // 等待持续时间：30秒（熔断器打开后，30秒后进入半开状态）
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // 半开状态允许的调用次数：3次（半开状态下允许3次试探请求）
                .permittedNumberOfCallsInHalfOpenState(3)
                // 自动从开启状态过渡到半开状态
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // 记录哪些异常
                .recordExceptions(Exception.class)
                // 不记录哪些异常（业务异常不触发熔断）
                .ignoreExceptions(IllegalArgumentException.class, IllegalStateException.class)
                .build();
            
            // 创建超时限制器配置
            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                // 超时时间：30秒
                .timeoutDuration(Duration.ofSeconds(30))
                .build();
            
            return new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(circuitBreakerConfig)
                .timeLimiterConfig(timeLimiterConfig)
                .build();
        });
    }
}

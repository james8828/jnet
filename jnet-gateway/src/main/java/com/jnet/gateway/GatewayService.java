package com.jnet.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients("com.jnet.api.feign")
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GatewayService
{
    public static void main( String[] args )
    {
        SpringApplication.run(GatewayService.class, args);
    }

    //@Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p
                        .path("/get")
                        .filters(f -> f.addRequestHeader("MyHeader", "MyURI").addRequestParameter("Param", "MyValue"))
                        .uri("http://httpbin.org:80"))
                .route("path_route", p -> p
                        .path("/first-service/**")
                        .uri("lb://first-service"))
                .route("rewrite_route", p -> p
                        .path("/rewrite-service/**")
                        .filters(f -> f.rewritePath("/rewrite-service/(?<segment>.*)", "/${segment}"))
                        .uri("lb://second-service"))
                .build();
    }
}

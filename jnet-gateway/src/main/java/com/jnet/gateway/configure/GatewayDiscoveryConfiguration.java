package com.jnet.gateway.configure;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/26 15:43:15
 */
@Configuration
public class GatewayDiscoveryConfiguration {

    /*@Bean
    public DiscoveryClientRouteDefinitionLocator
    discoveryClientRouteLocator(ReactiveDiscoveryClient discoveryClient) {

        return new DiscoveryClientRouteDefinitionLocator(discoveryClient);
    }*/
}

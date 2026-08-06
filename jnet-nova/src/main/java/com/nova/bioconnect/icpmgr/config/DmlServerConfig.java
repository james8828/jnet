package com.nova.bioconnect.icpmgr.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;

/**
 * DML Server Configuration
 */
@Configuration
@EnableConfigurationProperties(DmlServerProperties.class)
@EnableStateMachineFactory
public class DmlServerConfig {
}
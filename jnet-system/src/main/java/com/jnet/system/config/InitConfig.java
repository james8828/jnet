package com.jnet.system.config;

import com.jnet.common.core.security.SecurityComponentConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/22 18:06:35
 */
@Slf4j
@Configuration
@Import({SecurityComponentConfig.class})
@EnableWebSecurity
public class InitConfig {

    @Resource
    private DataSource dataSource;
    @PostConstruct
    public void init() throws Exception {
        log.info("dataSource:{}",dataSource);
        Connection connection = dataSource.getConnection();
        ScriptRunner runner = new ScriptRunner(connection);
        runner.setErrorLogWriter(null);
        runner.setLogWriter(null);
        runner.runScript(Resources.getResourceAsReader("db/migration/ddl.sql"));
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.disable())
                .headers((headers) -> headers.frameOptions(frameOptionsConfig -> frameOptionsConfig.sameOrigin()))
                .authorizeHttpRequests((authorize) -> {
                    authorize.anyRequest().permitAll();
                });
        return http.build();
    }
}

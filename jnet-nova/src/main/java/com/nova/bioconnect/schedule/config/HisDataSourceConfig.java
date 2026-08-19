package com.nova.bioconnect.schedule.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * HIS 数据库数据源配置
 */
@Configuration
public class HisDataSourceConfig {

    /**
     * HIS 数据库数据源（用于直接查询 HIS 数据库）
     */
    @Bean("hisDataSource")
    @ConditionalOnProperty(name = "bioconnect.sync.patient.strategy", havingValue = "database")
    public DataSource hisDataSource(SyncProperties properties) {
        SyncProperties.DatabaseConfig config = properties.getPatient().getDatabase();
        return DataSourceBuilder.create()
                .url(config.getUrl())
                .username(config.getUsername())
                .password(config.getPassword())
                .driverClassName(config.getDriverClassName())
                .build();
    }

    /**
     * HIS 数据库 JdbcTemplate
     */
    @Bean("hisJdbcTemplate")
    @ConditionalOnProperty(name = "bioconnect.sync.patient.strategy", havingValue = "database")
    public JdbcTemplate hisJdbcTemplate(@Qualifier("hisDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

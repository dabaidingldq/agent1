package com.ldq.hragent.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class BizDataSourceConfig {

    @Bean("bizDataSourceProperties")
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties bizDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("bizDataSource")
    @Primary
    public DataSource bizDataSource(
            @Qualifier("bizDataSourceProperties") DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean("bizJdbcTemplate")
    @Primary
    public JdbcTemplate bizJdbcTemplate(
            @Qualifier("bizDataSource") DataSource bizDataSource
    ) {
        return new JdbcTemplate(bizDataSource);
    }
}
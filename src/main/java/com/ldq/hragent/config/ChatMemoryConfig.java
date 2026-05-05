package com.ldq.hragent.config;

import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public JdbcChatMemoryRepository jdbcChatMemoryRepository(
            @Qualifier("bizJdbcTemplate") JdbcTemplate bizJdbcTemplate
    ) {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(bizJdbcTemplate)
                .dialect(new MysqlChatMemoryRepositoryDialect())
                .build();
    }
}
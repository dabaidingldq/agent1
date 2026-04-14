package com.yupi.yuaiagent.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final JdbcTemplate jdbcTemplate;

    public NotificationService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createNotification(Long userId, String title, String content, String type) {
        jdbcTemplate.update("""
                INSERT INTO notification_message
                (user_id, title, content, type, is_read, created_at, updated_at)
                VALUES (?, ?, ?, ?, 0, NOW(), NOW())
                """, userId, title, content, type);
    }
}
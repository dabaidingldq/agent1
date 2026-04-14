package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.chat.ChatMessageDTO;
import com.yupi.yuaiagent.model.chat.ChatSessionDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class ChatSessionService {

    private final JdbcTemplate jdbcTemplate;

    public ChatSessionService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createSessionIfAbsent(String chatId, Long userId, String roleName, String firstUserMessage) {
        Integer count = jdbcTemplate.query("""
                SELECT COUNT(1)
                FROM chat_session
                WHERE chat_id = ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, chatId);

        if (count != null && count > 0) {
            return;
        }

        String title = buildTitle(firstUserMessage);
        jdbcTemplate.update("""
                INSERT INTO chat_session
                (chat_id, user_id, role_name, title, last_message, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                """, chatId, userId, roleName, title, firstUserMessage);
    }

    public void saveUserMessage(String chatId, Long userId, String content) {
        jdbcTemplate.update("""
                INSERT INTO chat_message
                (chat_id, user_id, sender_type, content, created_at)
                VALUES (?, ?, 'USER', ?, NOW())
                """, chatId, userId, content);

        jdbcTemplate.update("""
                UPDATE chat_session
                SET last_message = ?, updated_at = NOW()
                WHERE chat_id = ?
                """, truncate(content, 500), chatId);
    }

    public void saveAssistantMessage(String chatId, Long userId, String content) {
        jdbcTemplate.update("""
                INSERT INTO chat_message
                (chat_id, user_id, sender_type, content, created_at)
                VALUES (?, ?, 'ASSISTANT', ?, NOW())
                """, chatId, userId, content);

        jdbcTemplate.update("""
                UPDATE chat_session
                SET last_message = ?, updated_at = NOW()
                WHERE chat_id = ?
                """, truncate(content, 500), chatId);
    }

    public List<ChatSessionDTO> listMySessions(Long userId, String roleName) {
        return jdbcTemplate.query("""
                SELECT chat_id, user_id, role_name, title, last_message, created_at, updated_at
                FROM chat_session
                WHERE user_id = ?
                  AND role_name = ?
                ORDER BY updated_at DESC
                LIMIT 100
                """, (rs, rowNum) -> new ChatSessionDTO(
                rs.getString("chat_id"),
                rs.getLong("user_id"),
                rs.getString("role_name"),
                rs.getString("title"),
                rs.getString("last_message"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ), userId, roleName);
    }

    public List<ChatMessageDTO> listMessages(String chatId, Long userId) {
        return jdbcTemplate.query("""
                SELECT id, chat_id, user_id, sender_type, content, created_at
                FROM chat_message
                WHERE chat_id = ?
                  AND user_id = ?
                ORDER BY created_at ASC, id ASC
                """, (rs, rowNum) -> new ChatMessageDTO(
                rs.getLong("id"),
                rs.getString("chat_id"),
                rs.getLong("user_id"),
                rs.getString("sender_type"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), chatId, userId);
    }

    private String buildTitle(String message) {
        if (message == null || message.isBlank()) {
            return "新对话";
        }
        String clean = message.replaceAll("\\s+", " ").trim();
        return clean.length() > 20 ? clean.substring(0, 20) + "..." : clean;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }
}
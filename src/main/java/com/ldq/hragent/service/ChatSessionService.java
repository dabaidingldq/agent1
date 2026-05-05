package com.ldq.hragent.service;

import com.ldq.hragent.model.chat.ChatMessageDTO;
import com.ldq.hragent.model.chat.ChatSessionDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatSessionService {

    private final JdbcTemplate jdbcTemplate;

    public ChatSessionService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 如果会话不存在，则创建会话。
     */
    public void createSessionIfAbsent(String chatId, Long userId, String roleName, String firstUserMessage) {
        Integer count = jdbcTemplate.query("""
                SELECT COUNT(1)
                FROM chat_session
                WHERE chat_id = ?
                  AND user_id = ?
                  AND role_name = ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, chatId, userId, roleName);

        if (count != null && count > 0) {
            return;
        }

        String title = buildTitle(firstUserMessage);

        jdbcTemplate.update("""
                INSERT INTO chat_session
                (chat_id, user_id, role_name, title, last_message, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                """,
                chatId,
                userId,
                roleName,
                title,
                truncate(firstUserMessage, 500)
        );
    }

    /**
     * 保存用户消息。
     */
    public void saveUserMessage(String chatId, Long userId, String content) {
        jdbcTemplate.update("""
                INSERT INTO chat_message
                (chat_id, user_id, sender_type, content, created_at)
                VALUES (?, ?, 'USER', ?, NOW())
                """, chatId, userId, content);

        jdbcTemplate.update("""
                UPDATE chat_session
                SET last_message = ?,
                    updated_at = NOW()
                WHERE chat_id = ?
                  AND user_id = ?
                """, truncate(content, 500), chatId, userId);
    }

    /**
     * 保存 AI 回复。
     */
    public void saveAssistantMessage(String chatId, Long userId, String content) {
        jdbcTemplate.update("""
                INSERT INTO chat_message
                (chat_id, user_id, sender_type, content, created_at)
                VALUES (?, ?, 'ASSISTANT', ?, NOW())
                """, chatId, userId, content);

        jdbcTemplate.update("""
                UPDATE chat_session
                SET last_message = ?,
                    updated_at = NOW()
                WHERE chat_id = ?
                  AND user_id = ?
                """, truncate(content, 500), chatId, userId);
    }

    /**
     * 查询当前登录用户、当前角色下的历史会话。
     */
    public List<ChatSessionDTO> listMySessions(Long userId, String roleName) {
        return jdbcTemplate.query("""
                SELECT chat_id,
                       user_id,
                       role_name,
                       title,
                       last_message,
                       created_at,
                       updated_at
                FROM chat_session
                WHERE user_id = ?
                  AND role_name = ?
                ORDER BY updated_at DESC
                LIMIT 100
                """,
                (rs, rowNum) -> new ChatSessionDTO(
                        rs.getString("chat_id"),
                        rs.getLong("user_id"),
                        rs.getString("role_name"),
                        rs.getString("title"),
                        rs.getString("last_message"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))
                ),
                userId,
                roleName
        );
    }

    /**
     * 查询某个会话下的消息。
     * 会校验 chat_id、user_id、role_name，防止用户越权看别人的会话。
     */
    public List<ChatMessageDTO> listMessages(String chatId, Long userId, String roleName) {
        return jdbcTemplate.query("""
                SELECT cm.id,
                       cm.chat_id,
                       cm.user_id,
                       cm.sender_type,
                       cm.content,
                       cm.created_at
                FROM chat_message cm
                INNER JOIN chat_session cs
                        ON cm.chat_id = cs.chat_id
                       AND cm.user_id = cs.user_id
                WHERE cm.chat_id = ?
                  AND cm.user_id = ?
                  AND cs.role_name = ?
                ORDER BY cm.created_at ASC, cm.id ASC
                """,
                (rs, rowNum) -> new ChatMessageDTO(
                        rs.getLong("id"),
                        rs.getString("chat_id"),
                        rs.getLong("user_id"),
                        rs.getString("sender_type"),
                        rs.getString("content"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                chatId,
                userId,
                roleName
        );
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.hr.NotificationDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.yupi.yuaiagent.model.hr.NotificationStatsResult;
import java.util.List;

@Service
public class NotificationQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public NotificationQueryService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public List<NotificationDTO> queryMyNotifications(int limit) {
        Long userId = permissionService.currentUserId();
        int actualLimit = Math.min(Math.max(limit, 1), 50);

        return jdbcTemplate.query("""
                SELECT id, title, content, type, is_read, created_at
                FROM notification_message
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT ?
                """, (rs, rowNum) -> new NotificationDTO(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("type"),
                rs.getBoolean("is_read"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId, actualLimit);
    }

    public String markAsRead(Long notificationId) {
        Long userId = permissionService.currentUserId();

        int rows = jdbcTemplate.update("""
                UPDATE notification_message
                SET is_read = 1, updated_at = NOW()
                WHERE id = ?
                  AND user_id = ?
                """, notificationId, userId);

        return rows > 0 ? "消息已标记为已读" : "未找到对应消息或无权限";
    }
    public NotificationStatsResult queryMyNotificationStats() {
        Long userId = permissionService.currentUserId();

        return jdbcTemplate.query("""
            SELECT COUNT(1) AS total_count,
                   SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS unread_count
            FROM notification_message
            WHERE user_id = ?
            """, rs -> {
            if (!rs.next()) {
                return new NotificationStatsResult(0, 0);
            }
            return new NotificationStatsResult(
                    rs.getInt("total_count"),
                    rs.getInt("unread_count")
            );
        }, userId);
    }

    public String markAllAsRead() {
        Long userId = permissionService.currentUserId();
        jdbcTemplate.update("""
            UPDATE notification_message
            SET is_read = 1, updated_at = NOW()
            WHERE user_id = ?
              AND is_read = 0
            """, userId);
        return "全部消息已标记为已读";
    }
}
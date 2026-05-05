package com.ldq.hragent.service;

import com.ldq.hragent.model.hr.PolicyAnnouncementResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;
    private final NotificationService notificationService;

    public AnnouncementService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                               PermissionService permissionService,
                               NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
        this.notificationService = notificationService;
    }
    @com.ldq.hragent.aop.AuditLog(operationName = "发布政策公告", operationModule = "announcement")
    public PolicyAnnouncementResult publishAnnouncement(String title, String summary, String targetScope) {
        permissionService.requireHrOrAdmin();
        Long creatorId = permissionService.currentUserId();

        String actualScope = (targetScope == null || targetScope.isBlank()) ? "ALL" : targetScope;

        jdbcTemplate.update("""
                INSERT INTO policy_announcement
                (title, summary, status, target_scope, created_by, created_at, updated_at)
                VALUES (?, ?, 'PUBLISHED', ?, ?, NOW(), NOW())
                """, title, summary, actualScope, creatorId);

        Long announcementId = jdbcTemplate.query("""
                SELECT id
                FROM policy_announcement
                WHERE created_by = ?
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, creatorId);

        List<Long> targetUserIds = switch (actualScope.toUpperCase()) {
            case "HR" -> jdbcTemplate.query("SELECT user_id FROM employee_profile WHERE cost_center = 'HR-OPS'", (rs, rowNum) -> rs.getLong(1));
            case "ADMIN" -> List.of(creatorId);
            default -> jdbcTemplate.query("SELECT user_id FROM employee_profile", (rs, rowNum) -> rs.getLong(1));
        };

        for (Long userId : targetUserIds) {
            notificationService.createNotification(
                    userId,
                    "政策公告：" + title,
                    summary,
                    "POLICY_ANNOUNCEMENT"
            );
        }

        return new PolicyAnnouncementResult(
                announcementId,
                title,
                "PUBLISHED",
                targetUserIds.size(),
                "政策公告已发布并推送。"
        );
    }
}
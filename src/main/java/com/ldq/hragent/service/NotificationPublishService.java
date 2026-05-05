package com.ldq.hragent.service;

import com.ldq.hragent.model.enums.ChatRole;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationPublishService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public NotificationPublishService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                                      PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public String publishToAll(String title, String content, String type) {
        permissionService.requireHrOrAdmin();

        Long senderUserId = permissionService.currentUserId();
        ChatRole senderRole = permissionService.currentRole();

        Set<Long> recipients = new LinkedHashSet<>(queryAllKnownUserIds());

        int count = batchInsertNotifications(new ArrayList<>(recipients), title, content, type, senderUserId, senderRole.name(), "ALL");
        return "已发送给 " + count + " 位用户";
    }

    public String publishToHrAndAdmin(String title, String content, String type) {
        permissionService.requireHrOrAdmin();

        Long senderUserId = permissionService.currentUserId();
        ChatRole senderRole = permissionService.currentRole();

        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(1L);     // admin
        recipients.add(2001L);  // hr

        int count = batchInsertNotifications(new ArrayList<>(recipients), title, content, type, senderUserId, senderRole.name(), "HR_ADMIN");
        return "已发送给 HR / ADMIN，共 " + count + " 条";
    }

    public String publishDirect(List<Long> recipientUserIds, String title, String content, String type) {
        permissionService.requireHrOrAdmin();

        Long senderUserId = permissionService.currentUserId();
        ChatRole senderRole = permissionService.currentRole();

        int count = batchInsertNotifications(recipientUserIds, title, content, type, senderUserId, senderRole.name(), "DIRECT");
        return "已发送 " + count + " 条定向消息";
    }

    public String publishSystemToHrAndAdmin(String title, String content, String type) {
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(1L);
        recipients.add(2001L);

        int count = batchInsertNotifications(new ArrayList<>(recipients), title, content, type, null, "SYSTEM", "HR_ADMIN");
        return "系统已发送 " + count + " 条提醒";
    }

    private List<Long> queryAllKnownUserIds() {
        List<Long> ids = jdbcTemplate.query(
                "SELECT DISTINCT user_id FROM employee_profile ORDER BY user_id",
                (rs, rowNum) -> rs.getLong("user_id")
        );
        Set<Long> all = new LinkedHashSet<>(ids);
        all.add(1L);
        all.add(2001L);
        return new ArrayList<>(all);
    }

    private int batchInsertNotifications(List<Long> recipientUserIds,
                                         String title,
                                         String content,
                                         String type,
                                         Long senderUserId,
                                         String senderRole,
                                         String audienceScope) {
        int count = 0;
        for (Long recipientUserId : recipientUserIds) {
            if (recipientUserId == null) {
                continue;
            }
            jdbcTemplate.update("""
                    INSERT INTO notification_message
                    (user_id, title, content, type, is_read, sender_user_id, sender_role, audience_scope, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 0, ?, ?, ?, NOW(), NOW())
                    """,
                    recipientUserId,
                    title,
                    content,
                    type,
                    senderUserId,
                    senderRole,
                    audienceScope
            );
            count++;
        }
        return count;
    }
}
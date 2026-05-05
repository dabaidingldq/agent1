package com.ldq.hragent.service;

import com.ldq.hragent.model.hr.VisitorRequestResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class VisitorService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public VisitorService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public VisitorRequestResult createVisitorRequest(String visitorName, String mobile, String visitDate, String visitReason) {
        permissionService.requireEmployeeOrAbove();
        Long userId = permissionService.currentUserId();

        if (visitorName == null || visitorName.isBlank()) {
            return new VisitorRequestResult(null, null, visitDate, "REJECTED", "访客姓名不能为空");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(visitDate);
        } catch (Exception e) {
            return new VisitorRequestResult(null, visitorName, visitDate, "REJECTED", "visitDate 格式不正确，请使用 yyyy-MM-dd");
        }

        String qrCodeContent = "VISITOR-" + userId + "-" + System.currentTimeMillis();

        jdbcTemplate.update("""
                INSERT INTO visitor_request
                (user_id, visitor_name, visitor_mobile, visit_date, visit_reason, qr_code_content, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', NOW(), NOW())
                """,
                userId,
                visitorName,
                mobile,
                date,
                visitReason,
                qrCodeContent
        );

        Long requestId = jdbcTemplate.query("""
                SELECT id
                FROM visitor_request
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, userId);

        return new VisitorRequestResult(
                requestId,
                visitorName,
                visitDate,
                "PENDING",
                "访客申请已提交，已生成临时访客码内容。后续可接短信 / 邮件发送。"
        );
    }
}
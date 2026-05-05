package com.ldq.hragent.service;

import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.model.hr.ApprovalHistoryResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalHistoryService {

    private final JdbcTemplate jdbcTemplate;

    private final PermissionService permissionService;

    public ApprovalHistoryService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                                  PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    /**
     * 查询当前登录用户自己发起的历史审批记录
     */
    public List<ApprovalHistoryResult> queryMyApprovalHistory(String businessType, String month) {
        Long userId = permissionService.currentUserId();

        String normalizedBusinessType = normalizeBlank(businessType);
        String normalizedMonth = normalizeBlank(month);

        String sql = """
                SELECT ai.id,
                       ai.business_type,
                       ai.status,
                       ai.current_node,
                       ep.employee_name,
                       ai.created_at,
                       ai.updated_at
                FROM approval_instance ai
                LEFT JOIN employee_profile ep ON ai.applicant_user_id = ep.user_id
                WHERE ai.applicant_user_id = ?
                  AND (? IS NULL OR ? = '' OR ai.business_type = ?)
                  AND (? IS NULL OR ? = '' OR DATE_FORMAT(ai.created_at, '%Y-%m') = ?)
                ORDER BY ai.id DESC
                LIMIT 50
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ApprovalHistoryResult(
                rs.getLong("id"),
                rs.getString("business_type"),
                rs.getString("status"),
                rs.getString("current_node"),
                rs.getString("employee_name"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        ), userId,
                normalizedBusinessType, normalizedBusinessType, normalizedBusinessType,
                normalizedMonth, normalizedMonth, normalizedMonth);
    }

    /**
     * 查询团队或全局历史审批记录
     */
    public List<ApprovalHistoryResult> queryTeamApprovalHistory(String businessType, String month) {
        permissionService.requireTeamLeadOrHrOrAdmin();

        ChatRole role = permissionService.currentRole();
        Long currentUserId = permissionService.currentUserId();

        String normalizedBusinessType = normalizeBlank(businessType);
        String normalizedMonth = normalizeBlank(month);

        String sql;
        Object[] args;

        if (role == ChatRole.HR || role == ChatRole.ADMIN) {
            sql = """
                    SELECT ai.id,
                           ai.business_type,
                           ai.status,
                           ai.current_node,
                           ep.employee_name,
                           ai.created_at,
                           ai.updated_at
                    FROM approval_instance ai
                    LEFT JOIN employee_profile ep ON ai.applicant_user_id = ep.user_id
                    WHERE (? IS NULL OR ? = '' OR ai.business_type = ?)
                      AND (? IS NULL OR ? = '' OR DATE_FORMAT(ai.created_at, '%Y-%m') = ?)
                    ORDER BY ai.id DESC
                    LIMIT 100
                    """;

            args = new Object[]{
                    normalizedBusinessType, normalizedBusinessType, normalizedBusinessType,
                    normalizedMonth, normalizedMonth, normalizedMonth
            };
        } else {
            sql = """
                    SELECT ai.id,
                           ai.business_type,
                           ai.status,
                           ai.current_node,
                           ep.employee_name,
                           ai.created_at,
                           ai.updated_at
                    FROM approval_instance ai
                    LEFT JOIN employee_profile ep ON ai.applicant_user_id = ep.user_id
                    WHERE ep.manager_user_id = ?
                      AND (? IS NULL OR ? = '' OR ai.business_type = ?)
                      AND (? IS NULL OR ? = '' OR DATE_FORMAT(ai.created_at, '%Y-%m') = ?)
                    ORDER BY ai.id DESC
                    LIMIT 100
                    """;

            args = new Object[]{
                    currentUserId,
                    normalizedBusinessType, normalizedBusinessType, normalizedBusinessType,
                    normalizedMonth, normalizedMonth, normalizedMonth
            };
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ApprovalHistoryResult(
                rs.getLong("id"),
                rs.getString("business_type"),
                rs.getString("status"),
                rs.getString("current_node"),
                rs.getString("employee_name"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        ), args);
    }

    private String normalizeBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
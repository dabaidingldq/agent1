package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.model.hr.ApprovalHistoryResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    public List<ApprovalHistoryResult> queryMyApprovalHistory(String businessType, String month) {
        Long userId = permissionService.currentUserId();

        String sql = """
                SELECT ai.id, ai.business_type, ai.status, ep.employee_name, ai.created_at
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
                rs.getString("employee_name"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId, businessType, businessType, businessType, month, month, month);
    }

    public List<ApprovalHistoryResult> queryTeamApprovalHistory(String businessType, String month) {
        permissionService.requireTeamLeadOrHrOrAdmin();
        ChatRole role = permissionService.currentRole();
        Long currentUserId = permissionService.currentUserId();

        String sql;
        Object[] args;

        if (role == ChatRole.HR || role == ChatRole.ADMIN) {
            sql = """
                    SELECT ai.id, ai.business_type, ai.status, ep.employee_name, ai.created_at
                    FROM approval_instance ai
                    LEFT JOIN employee_profile ep ON ai.applicant_user_id = ep.user_id
                    WHERE (? IS NULL OR ? = '' OR ai.business_type = ?)
                      AND (? IS NULL OR ? = '' OR DATE_FORMAT(ai.created_at, '%Y-%m') = ?)
                    ORDER BY ai.id DESC
                    LIMIT 100
                    """;
            args = new Object[]{businessType, businessType, businessType, month, month, month};
        } else {
            sql = """
                    SELECT ai.id, ai.business_type, ai.status, ep.employee_name, ai.created_at
                    FROM approval_instance ai
                    LEFT JOIN employee_profile ep ON ai.applicant_user_id = ep.user_id
                    WHERE ep.manager_user_id = ?
                      AND (? IS NULL OR ? = '' OR ai.business_type = ?)
                      AND (? IS NULL OR ? = '' OR DATE_FORMAT(ai.created_at, '%Y-%m') = ?)
                    ORDER BY ai.id DESC
                    LIMIT 100
                    """;
            args = new Object[]{currentUserId, businessType, businessType, businessType, month, month, month};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ApprovalHistoryResult(
                rs.getLong("id"),
                rs.getString("business_type"),
                rs.getString("status"),
                rs.getString("employee_name"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), args);
    }
}
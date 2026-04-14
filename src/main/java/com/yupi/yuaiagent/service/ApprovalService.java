package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.hr.ApprovalProgressResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public ApprovalService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public ApprovalProgressResult queryLatestMyApproval(String businessType) {
        Long userId = permissionService.currentUserId();

        String sql = """
                SELECT id, business_type, status, current_node, stay_days, estimated_remaining_time, can_remind
                FROM approval_instance
                WHERE applicant_user_id = ?
                  AND (? IS NULL OR business_type = ?)
                ORDER BY id DESC
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return new ApprovalProgressResult(
                        null,
                        businessType == null ? "UNKNOWN" : businessType,
                        "NOT_FOUND",
                        "无",
                        0,
                        "未知",
                        false
                );
            }
            return new ApprovalProgressResult(
                    rs.getLong("id"),
                    rs.getString("business_type"),
                    rs.getString("status"),
                    rs.getString("current_node"),
                    rs.getInt("stay_days"),
                    rs.getString("estimated_remaining_time"),
                    rs.getBoolean("can_remind")
            );
        }, userId, businessType, businessType);
    }

    public String remindCurrentApprover(Long approvalId) {
        Long userId = permissionService.currentUserId();

        String sql = """
                SELECT id, can_remind
                FROM approval_instance
                WHERE id = ?
                  AND applicant_user_id = ?
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return "未找到可催办的审批单，或该审批单不属于您。";
            }
            boolean canRemind = rs.getBoolean("can_remind");
            if (!canRemind) {
                return "当前审批单暂不支持催办。";
            }

            jdbcTemplate.update("""
                    UPDATE approval_instance
                    SET remind_count = COALESCE(remind_count, 0) + 1,
                        updated_at = NOW()
                    WHERE id = ?
                    """, approvalId);

            return "已向当前审批人发送温馨催办。";
        }, approvalId, userId);
    }
}
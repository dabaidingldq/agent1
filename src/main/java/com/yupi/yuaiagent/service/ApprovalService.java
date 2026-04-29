package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.hr.ApprovalProgressResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService {

    private final JdbcTemplate jdbcTemplate;

    private final PermissionService permissionService;

    private final NotificationPublishService notificationPublishService;

    public ApprovalService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                           PermissionService permissionService,
                           NotificationPublishService notificationPublishService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
        this.notificationPublishService = notificationPublishService;
    }

    /**
     * 查询当前登录用户最近一条审批进度
     */
    public ApprovalProgressResult queryLatestMyApproval(String businessType) {
        Long userId = permissionService.currentUserId();
        String normalizedBusinessType = normalizeBlank(businessType);

        String sql = """
                SELECT id,
                       business_type,
                       status,
                       current_node,
                       stay_days,
                       estimated_remaining_time,
                       can_remind
                FROM approval_instance
                WHERE applicant_user_id = ?
                  AND (? IS NULL OR ? = '' OR business_type = ?)
                ORDER BY id DESC
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return new ApprovalProgressResult(
                        null,
                        normalizedBusinessType == null || normalizedBusinessType.isBlank()
                                ? "UNKNOWN"
                                : normalizedBusinessType,
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
        }, userId, normalizedBusinessType, normalizedBusinessType, normalizedBusinessType);
    }

    /**
     * 催办当前登录用户本人发起的审批单
     */
    public String remindCurrentApprover(Long approvalId) {
        if (approvalId == null) {
            return "请提供需要催办的审批单 ID。";
        }

        Long userId = permissionService.currentUserId();

        String sql = """
                SELECT id,
                       can_remind,
                       business_type,
                       current_node,
                       status
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
            String businessType = rs.getString("business_type");
            String currentNode = rs.getString("current_node");
            String status = rs.getString("status");

            if (!"PENDING".equalsIgnoreCase(status)) {
                return "当前审批单不是待审批状态，无需催办。当前状态：" + status + "。";
            }

            if (!canRemind) {
                return "当前审批单暂不支持催办。";
            }

            jdbcTemplate.update("""
                    UPDATE approval_instance
                    SET remind_count = COALESCE(remind_count, 0) + 1,
                        updated_at = NOW()
                    WHERE id = ?
                    """, approvalId);

            notificationPublishService.publishSystemToHrAndAdmin(
                    "员工催办提醒",
                    "员工 userId=" + userId
                            + " 对审批单 #" + approvalId
                            + " 发起了催办。业务类型：" + businessType
                            + "，当前节点：" + currentNode + "。",
                    "APPROVAL_REMIND"
            );

            return "已向 HR / ADMIN 发送催办提醒。";
        }, approvalId, userId);
    }

    private String normalizeBlank(String value) {
        return value == null ? "" : value.trim();
    }
}
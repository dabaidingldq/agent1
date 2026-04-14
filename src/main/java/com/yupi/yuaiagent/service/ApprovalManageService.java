package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.exception.BizException;
import com.yupi.yuaiagent.model.enums.ChatRole;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalManageService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public ApprovalManageService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                                 PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public List<ApprovalManageItem> queryPendingApprovals(String businessType) {
        permissionService.requireHrOrAdmin();
        ChatRole role = permissionService.currentRole();

        String sql = """
                SELECT
                    ai.id,
                    ai.applicant_user_id,
                    COALESCE(ep.employee_name, CONCAT('用户', ai.applicant_user_id)) AS employee_name,
                    ai.business_type,
                    ai.status,
                    ai.current_node,
                    ai.created_at,
                    CASE
                        WHEN ai.business_type = 'LEAVE' THEN (
                            SELECT CONCAT(
                                COALESCE(l.leave_type, ''),
                                ' ',
                                DATE_FORMAT(l.start_time, '%%m-%%d %%H:%%i'),
                                ' ~ ',
                                DATE_FORMAT(l.end_time, '%%m-%%d %%H:%%i')
                            )
                            FROM leave_request l
                            WHERE l.user_id = ai.applicant_user_id
                            ORDER BY l.id DESC
                            LIMIT 1
                        )
                        WHEN ai.business_type = 'CERTIFICATE' THEN (
                            SELECT CONCAT(
                                COALESCE(c.certificate_type, ''),
                                ' / ',
                                COALESCE(c.purpose, '')
                            )
                            FROM certificate_request c
                            WHERE c.user_id = ai.applicant_user_id
                            ORDER BY c.id DESC
                            LIMIT 1
                        )
                        WHEN ai.business_type = 'OFFICE_SUPPLY' THEN (
                            SELECT LEFT(COALESCE(o.item_description, ''), 120)
                            FROM office_supply_order o
                            WHERE o.user_id = ai.applicant_user_id
                            ORDER BY o.id DESC
                            LIMIT 1
                        )
                        ELSE ''
                    END AS detail_summary
                FROM approval_instance ai
                LEFT JOIN employee_profile ep ON ep.user_id = ai.applicant_user_id
                WHERE ai.status IN ('PENDING', 'PENDING_REVIEW')
                  AND (? IS NULL OR ? = '' OR ai.business_type = ?)
                """;

        Object[] args;

        if (role == ChatRole.ADMIN) {
            sql += " ORDER BY ai.id DESC LIMIT 200";
            args = new Object[]{businessType, businessType, businessType};
        } else {
            sql += """
                    AND (
                        (ai.business_type = 'LEAVE' AND ai.current_node IN ('HR审批', 'HR审批'))
                        OR (ai.business_type = 'CERTIFICATE' AND ai.current_node IN ('HR复核', 'HR审批'))
                        OR (ai.business_type = 'OFFICE_SUPPLY' AND ai.current_node IN ('HR审批', 'HR审批'))
                    )
                    ORDER BY ai.id DESC
                    LIMIT 200
                    """;
            args = new Object[]{businessType, businessType, businessType};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ApprovalManageItem(
                rs.getLong("id"),
                rs.getLong("applicant_user_id"),
                rs.getString("employee_name"),
                rs.getString("business_type"),
                rs.getString("status"),
                rs.getString("current_node"),
                rs.getString("detail_summary"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        ), args);
    }

    public List<ApprovalManageItem> queryAllApprovals(String businessType, String status) {
        permissionService.requireHrOrAdmin();
        ChatRole role = permissionService.currentRole();

        String sql = """
                SELECT
                    ai.id,
                    ai.applicant_user_id,
                    COALESCE(ep.employee_name, CONCAT('用户', ai.applicant_user_id)) AS employee_name,
                    ai.business_type,
                    ai.status,
                    ai.current_node,
                    ai.created_at,
                    CASE
                        WHEN ai.business_type = 'LEAVE' THEN (
                            SELECT CONCAT(
                                COALESCE(l.leave_type, ''),
                                ' ',
                                DATE_FORMAT(l.start_time, '%%m-%%d %%H:%%i'),
                                ' ~ ',
                                DATE_FORMAT(l.end_time, '%%m-%%d %%H:%%i')
                            )
                            FROM leave_request l
                            WHERE l.user_id = ai.applicant_user_id
                            ORDER BY l.id DESC
                            LIMIT 1
                        )
                        WHEN ai.business_type = 'CERTIFICATE' THEN (
                            SELECT CONCAT(
                                COALESCE(c.certificate_type, ''),
                                ' / ',
                                COALESCE(c.purpose, '')
                            )
                            FROM certificate_request c
                            WHERE c.user_id = ai.applicant_user_id
                            ORDER BY c.id DESC
                            LIMIT 1
                        )
                        WHEN ai.business_type = 'OFFICE_SUPPLY' THEN (
                            SELECT LEFT(COALESCE(o.item_description, ''), 120)
                            FROM office_supply_order o
                            WHERE o.user_id = ai.applicant_user_id
                            ORDER BY o.id DESC
                            LIMIT 1
                        )
                        ELSE ''
                    END AS detail_summary
                FROM approval_instance ai
                LEFT JOIN employee_profile ep ON ep.user_id = ai.applicant_user_id
                WHERE (? IS NULL OR ? = '' OR ai.business_type = ?)
                  AND (? IS NULL OR ? = '' OR ai.status = ?)
                """;

        Object[] args;

        if (role == ChatRole.ADMIN) {
            sql += " ORDER BY ai.id DESC LIMIT 300";
            args = new Object[]{businessType, businessType, businessType, status, status, status};
        } else {
            sql += """
                    AND (
                        ai.business_type IN ('LEAVE', 'CERTIFICATE', 'OFFICE_SUPPLY')
                    )
                    ORDER BY ai.id DESC
                    LIMIT 300
                    """;
            args = new Object[]{businessType, businessType, businessType, status, status, status};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ApprovalManageItem(
                rs.getLong("id"),
                rs.getLong("applicant_user_id"),
                rs.getString("employee_name"),
                rs.getString("business_type"),
                rs.getString("status"),
                rs.getString("current_node"),
                rs.getString("detail_summary"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        ), args);
    }

    public String approveApproval(Long approvalId, String comment) {
        return handleApproval(approvalId, "APPROVED", "审批完成", comment);
    }

    public String rejectApproval(Long approvalId, String comment) {
        return handleApproval(approvalId, "REJECTED", "已驳回", comment);
    }

    private String handleApproval(Long approvalId, String targetStatus, String targetNode, String comment) {
        permissionService.requireHrOrAdmin();

        ApprovalRow row = jdbcTemplate.query("""
                SELECT id, applicant_user_id, business_type, status, current_node
                FROM approval_instance
                WHERE id = ?
                LIMIT 1
                """, rs -> {
            if (!rs.next()) {
                return null;
            }
            return new ApprovalRow(
                    rs.getLong("id"),
                    rs.getLong("applicant_user_id"),
                    rs.getString("business_type"),
                    rs.getString("status"),
                    rs.getString("current_node")
            );
        }, approvalId);

        if (row == null) {
            throw new BizException("审批单不存在");
        }

        if (!"PENDING".equalsIgnoreCase(row.status()) && !"PENDING_REVIEW".equalsIgnoreCase(row.status())) {
            throw new BizException("该审批单已处理，不能重复操作");
        }

        requireCanHandle(row.businessType(), row.currentNode());

        jdbcTemplate.update("""
                UPDATE approval_instance
                SET status = ?,
                    current_node = ?,
                    estimated_remaining_time = '0',
                    can_remind = 0,
                    updated_at = NOW()
                WHERE id = ?
                """, targetStatus, targetNode, approvalId);

        syncBusinessStatus(row.applicantUserId(), row.businessType(), targetStatus);

        String suffix = (comment == null || comment.isBlank()) ? "" : ("，备注：" + comment.trim());
        return "APPROVAL_SUCCESS: #" + approvalId + " 已更新为 " + targetStatus + suffix;
    }

    private void requireCanHandle(String businessType, String currentNode) {
        ChatRole role = permissionService.currentRole();
        if (role == ChatRole.ADMIN) {
            return;
        }

        boolean hrAllowedBusiness = "LEAVE".equalsIgnoreCase(businessType)
                || "CERTIFICATE".equalsIgnoreCase(businessType)
                || "OFFICE_SUPPLY".equalsIgnoreCase(businessType);

        boolean hrAllowedNode = "直属经理审批".equals(currentNode)
                || "HR审批".equals(currentNode)
                || "HR复核".equals(currentNode)
                || "行政审批".equals(currentNode);

        if (!hrAllowedBusiness || !hrAllowedNode) {
            throw new BizException("当前审批单不在 HR 可处理范围内");
        }
    }

    private void syncBusinessStatus(Long applicantUserId, String businessType, String targetStatus) {
        if ("LEAVE".equalsIgnoreCase(businessType)) {
            jdbcTemplate.update("""
                    UPDATE leave_request
                    SET status = ?, updated_at = NOW()
                    WHERE id = (
                        SELECT id FROM (
                            SELECT id
                            FROM leave_request
                            WHERE user_id = ?
                              AND status = 'PENDING'
                            ORDER BY id DESC
                            LIMIT 1
                        ) t
                    )
                    """, targetStatus, applicantUserId);
            return;
        }

        if ("CERTIFICATE".equalsIgnoreCase(businessType)) {
            jdbcTemplate.update("""
                    UPDATE certificate_request
                    SET status = ?, updated_at = NOW()
                    WHERE id = (
                        SELECT id FROM (
                            SELECT id
                            FROM certificate_request
                            WHERE user_id = ?
                              AND status IN ('PENDING', 'PENDING_REVIEW')
                            ORDER BY id DESC
                            LIMIT 1
                        ) t
                    )
                    """, targetStatus, applicantUserId);
            return;
        }

        if ("OFFICE_SUPPLY".equalsIgnoreCase(businessType)) {
            jdbcTemplate.update("""
                    UPDATE office_supply_order
                    SET status = ?, updated_at = NOW()
                    WHERE id = (
                        SELECT id FROM (
                            SELECT id
                            FROM office_supply_order
                            WHERE user_id = ?
                              AND status = 'PENDING'
                            ORDER BY id DESC
                            LIMIT 1
                        ) t
                    )
                    """, targetStatus, applicantUserId);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public record ApprovalManageItem(
            Long id,
            Long applicantUserId,
            String employeeName,
            String businessType,
            String status,
            String currentNode,
            String detailSummary,
            LocalDateTime createdAt
    ) {
    }

    private record ApprovalRow(
            Long id,
            Long applicantUserId,
            String businessType,
            String status,
            String currentNode
    ) {
    }
}
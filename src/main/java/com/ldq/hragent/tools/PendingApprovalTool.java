package com.ldq.hragent.tools;

import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.service.PermissionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PendingApprovalTool {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public PendingApprovalTool(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                               PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    @Tool(description = """
            查询待审批单列表。
            适用场景：
            - HR 或管理员说“查询待审批单”
            - HR 或管理员说“查询全部待审批单”
            - HR 或管理员说“有哪些待我处理的审批”
            - HR 或管理员说“查看待复核单据”
            
            参数规则：
            - businessType 可选：LEAVE、OFFICE_SUPPLY、CERTIFICATE、EXPENSE
            - 用户说“请假”时 businessType=LEAVE
            - 用户说“办公用品”时 businessType=OFFICE_SUPPLY
            - 用户说“证明/在职证明”时 businessType=CERTIFICATE
            - status 可选：PENDING、PENDING_REVIEW；如果用户没指定，默认查询 PENDING 和 PENDING_REVIEW
            - limit 默认 20，最大 100
            
            权限规则：
            - HR 可查询团队/全局待审批单
            - 管理员可查询全部待审批单
            - 普通员工不能查询全部待审批单
            """)
    public List<Map<String, Object>> queryPendingApprovals(String businessType, String status, Integer limit) {
        permissionService.requireTeamLeadOrHrOrAdmin();

        ChatRole role = permissionService.currentRole();
        Long currentUserId = permissionService.currentUserId();

        String normalizedBusinessType = normalizeBusinessType(businessType);
        String normalizedStatus = normalizeStatus(status);
        int actualLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);

        String sql;
        Object[] args;

        if (role == ChatRole.HR || role == ChatRole.ADMIN) {
            sql = """
                    SELECT ai.id,
                           ai.applicant_user_id,
                           ep.employee_name,
                           ai.business_type,
                           ai.status,
                           ai.current_node,
                           ai.created_at,
                           ai.updated_at
                    FROM approval_instance ai
                    LEFT JOIN employee_profile ep ON ai.applicant_user_id = ep.user_id
                    WHERE (? = '' OR ai.business_type = ?)
                      AND (
                            (? = '' AND ai.status IN ('PENDING', 'PENDING_REVIEW'))
                            OR (? <> '' AND ai.status = ?)
                          )
                    ORDER BY ai.created_at DESC, ai.id DESC
                    LIMIT ?
                    """;
            args = new Object[]{
                    normalizedBusinessType, normalizedBusinessType,
                    normalizedStatus,
                    normalizedStatus, normalizedStatus,
                    actualLimit
            };
        } else {
            sql = """
                    SELECT ai.id,
                           ai.applicant_user_id,
                           ep.employee_name,
                           ai.business_type,
                           ai.status,
                           ai.current_node,
                           ai.created_at,
                           ai.updated_at
                    FROM approval_instance ai
                    LEFT JOIN employee_profile ep ON ai.applicant_user_id = ep.user_id
                    WHERE ep.manager_user_id = ?
                      AND (? = '' OR ai.business_type = ?)
                      AND (
                            (? = '' AND ai.status IN ('PENDING', 'PENDING_REVIEW'))
                            OR (? <> '' AND ai.status = ?)
                          )
                    ORDER BY ai.created_at DESC, ai.id DESC
                    LIMIT ?
                    """;
            args = new Object[]{
                    currentUserId,
                    normalizedBusinessType, normalizedBusinessType,
                    normalizedStatus,
                    normalizedStatus, normalizedStatus,
                    actualLimit
            };
        }

        return jdbcTemplate.queryForList(sql, args);
    }

    private String normalizeBusinessType(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return "";
        }

        String text = businessType.trim().toUpperCase();

        if (text.contains("请假") || text.contains("LEAVE")) {
            return "LEAVE";
        }
        if (text.contains("办公") || text.contains("OFFICE")) {
            return "OFFICE_SUPPLY";
        }
        if (text.contains("证明") || text.contains("CERT")) {
            return "CERTIFICATE";
        }
        if (text.contains("报销") || text.contains("EXPENSE")) {
            return "EXPENSE";
        }

        return text;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String text = status.trim().toUpperCase();

        if (text.contains("复核") || text.contains("REVIEW")) {
            return "PENDING_REVIEW";
        }
        if (text.contains("待") || text.contains("PENDING")) {
            return "PENDING";
        }

        return text;
    }
}
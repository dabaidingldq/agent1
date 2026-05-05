package com.ldq.hragent.service;

import com.ldq.hragent.model.hr.OfficeSupplyApplyResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OfficeSupplyService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;
    private final EmployeeProfileService employeeProfileService;

    public OfficeSupplyService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                               PermissionService permissionService,
                               EmployeeProfileService employeeProfileService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
        this.employeeProfileService = employeeProfileService;
    }
    @com.ldq.hragent.aop.AuditLog(operationName = "提交办公用品申请", operationModule = "office_supply")
    public OfficeSupplyApplyResult applyOfficeSupplies(String items) {
        permissionService.requireEmployeeOrAbove();
        Long userId = permissionService.currentUserId();

        Map<String, Object> profile = employeeProfileService.getBasicProfileForOfficeSupply();
        String employeeName = String.valueOf(profile.getOrDefault("employeeName", "未知员工"));
        String costCenter = String.valueOf(profile.getOrDefault("costCenter", "DEFAULT"));
        String mobile = String.valueOf(profile.getOrDefault("mobile", ""));
        String address = String.valueOf(profile.getOrDefault("address", ""));

        // 这里先做最小规则校验，后续可扩展预算和品类规则
        if (items == null || items.isBlank()) {
            return new OfficeSupplyApplyResult(null, "REJECTED", "申请内容不能为空");
        }

        if (containsRestrictedCategory(items)) {
            return new OfficeSupplyApplyResult(null, "REJECTED", "当前申请包含限制品类，请联系行政处理。");
        }

        String insertSql = """
                INSERT INTO office_supply_order
                (user_id, employee_name, item_description, cost_center, receiver_mobile, receiver_address, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;

        jdbcTemplate.update(insertSql,
                userId,
                employeeName,
                items,
                costCenter,
                mobile,
                address,
                "PENDING");

        Long orderId = jdbcTemplate.query("""
                SELECT id
                FROM office_supply_order
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, userId);

        // 同步插入审批单
        jdbcTemplate.update("""
                INSERT INTO approval_instance
                (applicant_user_id, business_type, status, current_node, stay_days, estimated_remaining_time, can_remind, created_at, updated_at)
                VALUES (?, 'OFFICE_SUPPLY', 'PENDING', '行政审批', 0, '1个工作日', 1, NOW(), NOW())
                """, userId);

        return new OfficeSupplyApplyResult(
                orderId,
                "PENDING",
                "办公用品申请已提交，已自动带出收货信息和成本中心。"
        );
    }

    private boolean containsRestrictedCategory(String items) {
        String normalized = items.toLowerCase();
        return normalized.contains("手机")
                || normalized.contains("电脑")
                || normalized.contains("显示器")
                || normalized.contains("高价值设备");
    }
}
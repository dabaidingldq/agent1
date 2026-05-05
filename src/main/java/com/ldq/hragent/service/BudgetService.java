package com.ldq.hragent.service;

import com.ldq.hragent.aop.AuditLog;
import com.ldq.hragent.model.hr.BudgetWarningResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BudgetService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public BudgetService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public BudgetWarningResult queryBudgetUsage(String costCenter) {
        permissionService.requireAdmin();

        return jdbcTemplate.query("""
                SELECT cost_center, total_budget, used_budget, warning_threshold
                FROM department_budget
                WHERE cost_center = ?
                LIMIT 1
                """, rs -> {
            if (!rs.next()) {
                return new BudgetWarningResult(costCenter, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
            }
            BigDecimal total = rs.getBigDecimal("total_budget");
            BigDecimal used = rs.getBigDecimal("used_budget");
            BigDecimal usageRate = BigDecimal.ZERO;
            if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                usageRate = used.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
            }
            BigDecimal threshold = rs.getBigDecimal("warning_threshold");
            boolean warning = usageRate.compareTo(threshold) >= 0;
            return new BudgetWarningResult(
                    rs.getString("cost_center"),
                    total,
                    used,
                    usageRate,
                    warning
            );
        }, costCenter);
    }

    @AuditLog(operationName = "更新预算使用额", operationModule = "budget")
    public String increaseUsedBudget(String costCenter, BigDecimal amount) {
        permissionService.requireAdmin();
        jdbcTemplate.update("""
                UPDATE department_budget
                SET used_budget = used_budget + ?, updated_at = NOW()
                WHERE cost_center = ?
                """, amount, costCenter);
        return "预算使用金额已更新";
    }
}
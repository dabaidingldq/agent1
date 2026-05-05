package com.ldq.hragent.service;

import com.ldq.hragent.model.hr.PersonalDashboardResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public DashboardService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public PersonalDashboardResult queryMyDashboard() {
        Long userId = permissionService.currentUserId();

        BigDecimal annualLeaveUsageRate = jdbcTemplate.query("""
                SELECT COALESCE(annual_leave_usage_rate, 0)
                FROM employee_leave_balance
                WHERE user_id = ?
                LIMIT 1
                """, rs -> rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO, userId);

        BigDecimal monthlyOvertimeHours = jdbcTemplate.query("""
                SELECT COALESCE(SUM(hours), 0)
                FROM overtime_request
                WHERE user_id = ?
                  AND DATE_FORMAT(overtime_date, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')
                """, rs -> rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO, userId);

        Integer pendingApprovals = jdbcTemplate.query("""
                SELECT COUNT(1)
                FROM approval_instance
                WHERE applicant_user_id = ?
                  AND status = 'PENDING'
                """, rs -> rs.next() ? rs.getInt(1) : 0, userId);

        Integer pendingTasks = pendingApprovals;

        return new PersonalDashboardResult(
                annualLeaveUsageRate == null ? BigDecimal.ZERO : annualLeaveUsageRate,
                monthlyOvertimeHours == null ? BigDecimal.ZERO : monthlyOvertimeHours,
                pendingApprovals == null ? 0 : pendingApprovals,
                pendingTasks
        );
    }
}
package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.hr.TeamOvertimeStatResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamAnalyticsService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public TeamAnalyticsService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                                PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public List<TeamOvertimeStatResult> queryWeeklyTopOvertimeEmployees(int limit) {
        permissionService.requireHrOrAdmin();

        String sql = """
                SELECT o.user_id,
                       p.employee_name,
                       COALESCE(SUM(o.hours), 0) AS overtime_hours
                FROM overtime_request o
                LEFT JOIN employee_profile p ON o.user_id = p.user_id
                WHERE YEARWEEK(o.overtime_date, 1) = YEARWEEK(CURDATE(), 1)
                GROUP BY o.user_id, p.employee_name
                ORDER BY overtime_hours DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeamOvertimeStatResult(
                rs.getLong("user_id"),
                rs.getString("employee_name"),
                rs.getBigDecimal("overtime_hours")
        ), limit);
    }
}
package com.ldq.hragent.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmployeeProfileService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public EmployeeProfileService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public String getEmployeeName() {
        Long userId = permissionService.currentUserId();
        String sql = """
                SELECT employee_name
                FROM employee_profile
                WHERE user_id = ?
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString("employee_name") : "员工", userId);
    }

    public Map<String, Object> getBasicProfileForOfficeSupply() {
        Long userId = permissionService.currentUserId();
        String sql = """
                SELECT employee_name, mobile, address, cost_center
                FROM employee_profile
                WHERE user_id = ?
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return Map.of(
                        "employeeName", "未知员工",
                        "mobile", "",
                        "address", "",
                        "costCenter", "DEFAULT"
                );
            }
            return Map.of(
                    "employeeName", rs.getString("employee_name"),
                    "mobile", rs.getString("mobile"),
                    "address", rs.getString("address"),
                    "costCenter", rs.getString("cost_center")
            );
        }, userId);
    }
}
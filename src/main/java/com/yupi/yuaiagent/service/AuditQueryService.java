package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.audit.AuditLogDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public AuditQueryService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public List<AuditLogDTO> queryLatestLogs(int limit) {
        permissionService.requireAdmin();
        int actualLimit = Math.min(Math.max(limit, 1), 100);

        return jdbcTemplate.query("""
                SELECT id, user_id, role_name, operation_name, operation_module, result_summary, success_flag, created_at
                FROM audit_log
                ORDER BY id DESC
                LIMIT ?
                """, (rs, rowNum) -> new AuditLogDTO(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("role_name"),
                rs.getString("operation_name"),
                rs.getString("operation_module"),
                rs.getString("result_summary"),
                rs.getBoolean("success_flag"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), actualLimit);
    }
}
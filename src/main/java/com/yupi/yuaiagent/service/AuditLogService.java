package com.yupi.yuaiagent.service;

import com.yupi.yuaiagent.model.audit.AuditLogRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(AuditLogRecord record) {
        jdbcTemplate.update("""
                INSERT INTO audit_log
                (user_id, role_name, operation_name, operation_module, request_params, result_summary, success_flag, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                record.getUserId(),
                record.getRoleName(),
                record.getOperationName(),
                record.getOperationModule(),
                record.getRequestParams(),
                record.getResultSummary(),
                Boolean.TRUE.equals(record.getSuccessFlag()) ? 1 : 0
        );
    }
}
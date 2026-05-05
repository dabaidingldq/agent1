package com.ldq.hragent.service;

import com.ldq.hragent.model.audit.AuditLogRecord;
import com.ldq.hragent.model.auth.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务
 *
 * 注意：
 * 审计日志是辅助能力，不能因为审计日志写入失败影响主业务。
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 给 AuditLogAspect 使用。
     */
    public void save(AuditLogRecord record) {
        if (record == null) {
            return;
        }

        try {
            doInsert(record);
        } catch (Exception e) {
            log.error("保存审计日志失败，已忽略，不影响主业务。record={}", record, e);
        }
    }

    /**
     * 实际写入数据库。
     *
     * 注意：这里使用 success_flag。
     * 如果你的 audit_log 表字段不是 success_flag，请看下面第二部分的 SQL 检查。
     */
    private void doInsert(AuditLogRecord record) {
        jdbcTemplate.update("""
                INSERT INTO audit_log
                (user_id, role_name, operation_name, operation_module, request_params, result_summary, success_flag, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                record.getUserId(),
                safe(record.getRoleName(), 32),
                safe(record.getOperationName(), 100),
                safe(record.getOperationModule(), 100),
                safe(record.getRequestParams(), 1000),
                safe(record.getResultSummary(), 1000),
                Boolean.TRUE.equals(record.getSuccessFlag()) ? 1 : 0
        );
    }

    /**
     * 给 Controller / Service 手动记录审计日志使用。
     */
    public void record(LoginUser loginUser,
                       String operationName,
                       String operationModule,
                       String requestParams,
                       String resultSummary,
                       boolean success) {
        if (loginUser == null) {
            record(
                    null,
                    "UNKNOWN",
                    operationName,
                    operationModule,
                    requestParams,
                    resultSummary,
                    success
            );
            return;
        }

        record(
                loginUser.getUserId(),
                loginUser.getRole() == null ? "UNKNOWN" : loginUser.getRole().name(),
                operationName,
                operationModule,
                requestParams,
                resultSummary,
                success
        );
    }

    public void record(Long userId,
                       String roleName,
                       String operationName,
                       String operationModule,
                       String requestParams,
                       String resultSummary,
                       boolean success) {
        AuditLogRecord record = AuditLogRecord.builder()
                .userId(userId)
                .roleName(roleName)
                .operationName(operationName)
                .operationModule(operationModule)
                .requestParams(requestParams)
                .resultSummary(resultSummary)
                .successFlag(success)
                .build();

        save(record);
    }

    public String summarize(Object value) {
        if (value == null) {
            return "";
        }
        return safe(String.valueOf(value), 1000);
    }

    private String safe(String text, int maxLen) {
        if (text == null) {
            return "";
        }

        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }
}
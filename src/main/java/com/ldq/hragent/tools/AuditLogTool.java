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
public class AuditLogTool {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public AuditLogTool(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate,
                        PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    @Tool(description = """
            查询最近的系统审计日志，仅管理员可用。
            适用场景：
            - 管理员说“查看最近审计日志”
            - 管理员说“查询操作日志”
            - 管理员说“最近有哪些系统操作记录”
            - 管理员说“审计日志”
            
            参数规则：
            - limit 默认 20，最大 100
            - 只查询最近日志，按 id 倒序
            - HR 和普通员工无权查询系统审计日志
            """)
    public List<Map<String, Object>> queryRecentAuditLogs(Integer limit) {
        if (permissionService.currentRole() != ChatRole.ADMIN) {
            return List.of(Map.of(
                    "message", "当前角色无权查询系统审计日志，审计日志仅管理员可访问。"
            ));
        }

        int actualLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);

        return jdbcTemplate.queryForList("""
                SELECT *
                FROM audit_log
                ORDER BY id DESC
                LIMIT ?
                """, actualLimit);
    }
}
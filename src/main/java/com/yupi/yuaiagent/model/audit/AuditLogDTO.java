package com.yupi.yuaiagent.model.audit;

import java.time.LocalDateTime;

public record AuditLogDTO(
        Long id,
        Long userId,
        String roleName,
        String operationName,
        String operationModule,
        String resultSummary,
        Boolean successFlag,
        LocalDateTime createdAt
) {
}
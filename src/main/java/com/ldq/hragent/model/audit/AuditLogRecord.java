package com.ldq.hragent.model.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计日志记录对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRecord {

    private Long userId;

    private String roleName;

    private String operationName;

    private String operationModule;

    private String requestParams;

    private String resultSummary;

    private Boolean successFlag;
}
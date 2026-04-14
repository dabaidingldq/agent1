package com.yupi.yuaiagent.model.audit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogRecord {

    private Long userId;

    private String roleName;

    private String operationName;

    private String operationModule;

    private String requestParams;

    private String resultSummary;

    private Boolean successFlag;
}
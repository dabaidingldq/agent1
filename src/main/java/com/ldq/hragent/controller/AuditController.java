package com.ldq.hragent.controller;

import com.ldq.hragent.common.BaseResponse;
import com.ldq.hragent.context.HrRequestContext;
import com.ldq.hragent.context.HrRequestContextHolder;
import com.ldq.hragent.model.audit.AuditLogDTO;
import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.service.AuditQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/audit")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/latest")
    public BaseResponse<List<AuditLogDTO>> latest(@RequestParam Long userId,
                                                  @RequestParam(defaultValue = "20") Integer limit) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("audit-query")
                    .role(ChatRole.ADMIN)
                    .build());
            return BaseResponse.success(auditQueryService.queryLatestLogs(limit));
        } finally {
            HrRequestContextHolder.clear();
        }
    }
}
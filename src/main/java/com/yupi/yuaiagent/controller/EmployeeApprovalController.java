package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.model.hr.ApprovalHistoryResult;
import com.yupi.yuaiagent.model.hr.ApprovalProgressResult;
import com.yupi.yuaiagent.service.ApprovalHistoryService;
import com.yupi.yuaiagent.service.ApprovalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employee/approvals")
public class EmployeeApprovalController {

    private final ApprovalHistoryService approvalHistoryService;
    private final ApprovalService approvalService;

    public EmployeeApprovalController(ApprovalHistoryService approvalHistoryService,
                                      ApprovalService approvalService) {
        this.approvalHistoryService = approvalHistoryService;
        this.approvalService = approvalService;
    }

    @GetMapping("/latest")
    public BaseResponse<ApprovalProgressResult> latest(@RequestParam Long userId,
                                                       @RequestParam(required = false) String businessType) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("employee-approval-latest")
                    .role(ChatRole.EMPLOYEE)
                    .build());

            return BaseResponse.success(approvalService.queryLatestMyApproval(businessType));
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @GetMapping("/history")
    public BaseResponse<List<ApprovalHistoryResult>> history(@RequestParam Long userId,
                                                             @RequestParam(required = false) String businessType,
                                                             @RequestParam(required = false) String month,
                                                             @RequestParam(required = false) String status) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("employee-approval-history")
                    .role(ChatRole.EMPLOYEE)
                    .build());

            List<ApprovalHistoryResult> list = approvalHistoryService.queryMyApprovalHistory(businessType, month);

            if (status != null && !status.isBlank()) {
                list = list.stream()
                        .filter(item -> status.equalsIgnoreCase(item.status()))
                        .toList();
            }

            return BaseResponse.success(list);
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @PostMapping("/{approvalId}/remind")
    public BaseResponse<String> remind(@PathVariable Long approvalId,
                                       @RequestParam Long userId) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("employee-approval-remind")
                    .role(ChatRole.EMPLOYEE)
                    .build());

            return BaseResponse.success(approvalService.remindCurrentApprover(approvalId));
        } finally {
            HrRequestContextHolder.clear();
        }
    }
}
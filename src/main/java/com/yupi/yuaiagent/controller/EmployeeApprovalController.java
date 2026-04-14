package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.model.hr.ApprovalHistoryResult;
import com.yupi.yuaiagent.service.ApprovalHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employee/approvals")
public class EmployeeApprovalController {

    private final ApprovalHistoryService approvalHistoryService;

    public EmployeeApprovalController(ApprovalHistoryService approvalHistoryService) {
        this.approvalHistoryService = approvalHistoryService;
    }

    @GetMapping("/history")
    public BaseResponse<List<ApprovalHistoryResult>> history(@RequestParam Long userId,
                                                             @RequestParam(required = false) String businessType,
                                                             @RequestParam(required = false) String month) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("employee-approval-history")
                    .role(ChatRole.EMPLOYEE)
                    .build());
            return BaseResponse.success(
                    approvalHistoryService.queryMyApprovalHistory(businessType, month)
            );
        } finally {
            HrRequestContextHolder.clear();
        }
    }
}
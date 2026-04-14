package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.model.hr.BudgetWarningResult;
import com.yupi.yuaiagent.service.BudgetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/budget")
public class AdminBudgetController {

    private final BudgetService budgetService;

    public AdminBudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/usage")
    public BaseResponse<BudgetWarningResult> usage(@RequestParam Long userId,
                                                   @RequestParam String costCenter) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("admin-budget-usage")
                    .role(ChatRole.ADMIN)
                    .build());
            return BaseResponse.success(
                    budgetService.queryBudgetUsage(costCenter)
            );
        } finally {
            HrRequestContextHolder.clear();
        }
    }
}
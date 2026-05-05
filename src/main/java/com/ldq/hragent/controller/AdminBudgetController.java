package com.ldq.hragent.controller;

import com.ldq.hragent.common.BaseResponse;
import com.ldq.hragent.context.HrRequestContext;
import com.ldq.hragent.context.HrRequestContextHolder;
import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.model.hr.BudgetWarningResult;
import com.ldq.hragent.service.BudgetService;
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
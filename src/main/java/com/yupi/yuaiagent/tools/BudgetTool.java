package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.BudgetWarningResult;
import com.yupi.yuaiagent.service.BudgetService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BudgetTool {

    private final BudgetService budgetService;

    public BudgetTool(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Tool(description = "查询某个成本中心的预算使用情况，仅管理员可用")
    public BudgetWarningResult queryBudgetUsage(String costCenter) {
        return budgetService.queryBudgetUsage(costCenter);
    }

    @Tool(description = "增加某个成本中心的预算使用金额，仅管理员可用")
    public String increaseUsedBudget(String costCenter, BigDecimal amount) {
        return budgetService.increaseUsedBudget(costCenter, amount);
    }
}
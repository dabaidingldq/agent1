package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.ExpenseDraftResult;
import com.yupi.yuaiagent.service.ExpenseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpenseTool {

    private final ExpenseService expenseService;

    public ExpenseTool(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Tool(description = "创建当前登录员工的报销单草稿。amount 为报销金额，invoiceTitle 为发票抬头，expenseType 为费用类型")
    public ExpenseDraftResult createExpenseDraft(BigDecimal amount, String invoiceTitle, String expenseType, String remark) {
        return expenseService.createExpenseDraft(amount, invoiceTitle, expenseType, remark);
    }
}
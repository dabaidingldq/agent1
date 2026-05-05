package com.ldq.hragent.tools;

import com.ldq.hragent.model.hr.ExpenseDraftResult;
import com.ldq.hragent.service.ExpenseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpenseTool {

    private final ExpenseService expenseService;

    public ExpenseTool(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Tool(description = """
            创建当前登录员工的报销单草稿。
            适用场景：
            - 用户说“帮我创建报销草稿”
            - 用户说“我要报销交通费 / 餐费 / 差旅费”
            - 用户通过快捷入口提交报销信息
            
            参数规则：
            - amount：报销金额，必须是数字，例如 120.50
            - invoiceTitle：发票抬头
            - expenseType：费用类型，例如 交通、餐饮、差旅、办公采购
            - remark：报销说明或备注
            - 如果用户缺少金额或费用类型，应先追问
            - 如果用户只是问“报销标准是什么”，应优先查知识库，不应直接创建草稿
            - 工具只创建草稿，不等于最终审批通过
            """)
    public ExpenseDraftResult createExpenseDraft(BigDecimal amount, String invoiceTitle, String expenseType, String remark) {
        return expenseService.createExpenseDraft(amount, invoiceTitle, expenseType, remark);
    }
}
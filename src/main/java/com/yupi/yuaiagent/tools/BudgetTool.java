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

    @Tool(description = """
            查询某个成本中心的预算使用情况，仅管理员可用。
            适用场景：
            - 管理员说“查询预算使用情况”
            - 管理员说“查看某成本中心预算”
            - 管理员说“成本中心 CC001 预算用了多少”
            
            参数规则：
            - costCenter：成本中心编码或名称
            - 如果用户没有提供成本中心，应先追问
            - 普通员工和 HR 不应调用此工具
            """)
    public BudgetWarningResult queryBudgetUsage(String costCenter) {
        return budgetService.queryBudgetUsage(costCenter);
    }

    @Tool(description = """
            增加某个成本中心的预算使用金额，仅管理员可用。
            适用场景：
            - 管理员明确说“增加预算使用金额”
            - 管理员明确说“登记某成本中心用了多少钱”
            
            参数规则：
            - costCenter：成本中心编码或名称
            - amount：增加的使用金额，必须是数字
            - 如果缺少成本中心或金额，应先追问
            - 这是写操作，不要在用户只是查询或咨询时调用
            """)
    public String increaseUsedBudget(String costCenter, BigDecimal amount) {
        return budgetService.increaseUsedBudget(costCenter, amount);
    }
}
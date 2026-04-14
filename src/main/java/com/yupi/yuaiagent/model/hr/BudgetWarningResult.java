package com.yupi.yuaiagent.model.hr;

import java.math.BigDecimal;

public record BudgetWarningResult(
        String costCenter,
        BigDecimal totalBudget,
        BigDecimal usedBudget,
        BigDecimal usageRate,
        boolean warning
) {
}
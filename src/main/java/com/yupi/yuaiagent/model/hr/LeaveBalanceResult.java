package com.yupi.yuaiagent.model.hr;

import java.math.BigDecimal;

public record LeaveBalanceResult(
        BigDecimal annualLeaveRemaining,
        BigDecimal personalLeaveUsed,
        BigDecimal sickLeaveUsed,
        BigDecimal compOffRemaining,
        String policySummary
) {
}
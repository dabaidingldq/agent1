package com.ldq.hragent.model.hr;

import java.math.BigDecimal;

public record PersonalDashboardResult(
        BigDecimal annualLeaveUsageRate,
        BigDecimal monthlyOvertimeHours,
        Integer pendingApprovals,
        Integer pendingTasks
) {
}
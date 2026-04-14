package com.yupi.yuaiagent.model.hr;

import java.math.BigDecimal;

public record TeamOvertimeStatResult(
        Long userId,
        String employeeName,
        BigDecimal overtimeHours
) {
}
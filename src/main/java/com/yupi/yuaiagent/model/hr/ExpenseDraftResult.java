package com.yupi.yuaiagent.model.hr;

import java.math.BigDecimal;

public record ExpenseDraftResult(
        Long draftId,
        BigDecimal amount,
        String invoiceTitle,
        String status,
        String message
) {
}
package com.yupi.yuaiagent.model.hr;

public record OfficeSupplyApplyResult(
        Long orderId,
        String status,
        String message
) {
}
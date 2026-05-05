package com.ldq.hragent.model.hr;

public record OfficeSupplyApplyResult(
        Long orderId,
        String status,
        String message
) {
}
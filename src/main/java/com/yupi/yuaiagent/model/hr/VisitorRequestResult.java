package com.yupi.yuaiagent.model.hr;

public record VisitorRequestResult(
        Long requestId,
        String visitorName,
        String visitDate,
        String status,
        String message
) {
}
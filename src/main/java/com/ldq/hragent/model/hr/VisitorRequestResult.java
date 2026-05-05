package com.ldq.hragent.model.hr;

public record VisitorRequestResult(
        Long requestId,
        String visitorName,
        String visitDate,
        String status,
        String message
) {
}
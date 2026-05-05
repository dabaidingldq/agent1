package com.ldq.hragent.model.hr;

public record CertificateApplyResult(
        Long requestId,
        String certificateType,
        String status,
        String message
) {
}
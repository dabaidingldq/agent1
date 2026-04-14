package com.yupi.yuaiagent.model.hr;

public record CertificateApplyResult(
        Long requestId,
        String certificateType,
        String status,
        String message
) {
}
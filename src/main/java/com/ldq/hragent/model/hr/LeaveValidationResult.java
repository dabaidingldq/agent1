package com.ldq.hragent.model.hr;

public record LeaveValidationResult(
        boolean valid,
        String message,
        boolean conflictDetected,
        boolean balanceEnough
) {
}
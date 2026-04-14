package com.yupi.yuaiagent.model.hr;

public record LeaveValidationResult(
        boolean valid,
        String message,
        boolean conflictDetected,
        boolean balanceEnough
) {
}
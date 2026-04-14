package com.yupi.yuaiagent.model.hr;

public record LeaveRequestResult(
        Long requestId,
        String leaveType,
        String startTime,
        String endTime,
        String status,
        String message
) {
}
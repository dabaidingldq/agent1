package com.yupi.yuaiagent.model.hr;

import java.time.LocalDateTime;

public record ApprovalHistoryResult(
        Long approvalId,
        String businessType,
        String status,
        String currentNode,
        String applicantName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
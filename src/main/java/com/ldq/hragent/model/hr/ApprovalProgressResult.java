package com.ldq.hragent.model.hr;

public record ApprovalProgressResult(
        Long approvalId,
        String businessType,
        String status,
        String currentNode,
        Integer stayDays,
        String estimatedRemainingTime,
        boolean canRemind
) {
}
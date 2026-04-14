package com.yupi.yuaiagent.model.hr;

public record PolicyAnnouncementResult(
        Long announcementId,
        String title,
        String status,
        Integer targetUserCount,
        String message
) {
}
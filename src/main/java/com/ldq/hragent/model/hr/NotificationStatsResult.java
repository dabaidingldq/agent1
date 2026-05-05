package com.ldq.hragent.model.hr;

public record NotificationStatsResult(
        Integer totalCount,
        Integer unreadCount
) {
}
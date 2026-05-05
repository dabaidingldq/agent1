package com.ldq.hragent.model.hr;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        String title,
        String content,
        String type,
        Boolean isRead,
        LocalDateTime createdAt
) {
}
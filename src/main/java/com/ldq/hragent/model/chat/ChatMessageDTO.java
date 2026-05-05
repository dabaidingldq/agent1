package com.ldq.hragent.model.chat;

import java.time.LocalDateTime;

public record ChatMessageDTO(
        Long id,
        String chatId,
        Long userId,
        String senderType,
        String content,
        LocalDateTime createdAt
) {
}
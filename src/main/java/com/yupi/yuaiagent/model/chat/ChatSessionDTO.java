package com.yupi.yuaiagent.model.chat;

import java.time.LocalDateTime;

public record ChatSessionDTO(
        String chatId,
        Long userId,
        String roleName,
        String title,
        String lastMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
package com.yupi.yuaiagent.model.dto;

import com.yupi.yuaiagent.model.enums.ChatRole;
import lombok.Data;

@Data
public class ChatRequest {

    /**
     * 会话 id
     */
    private String chatId;

    /**
     * 当前用户 id
     */
    private Long userId;

    /**
     * 租户 id / 公司 id
     */
    private String tenantId;

    /**
     * 当前角色：EMPLOYEE / HR / ADMIN
     */
    private ChatRole role;

    /**
     * 用户输入
     */
    private String message;
}
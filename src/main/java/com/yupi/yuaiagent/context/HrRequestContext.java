package com.yupi.yuaiagent.context;

import com.yupi.yuaiagent.model.enums.ChatRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HrRequestContext {

    private String chatId;

    private Long userId;

    private String tenantId;

    private ChatRole role;
}
package com.ldq.hragent.context;

import com.ldq.hragent.model.enums.ChatRole;
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
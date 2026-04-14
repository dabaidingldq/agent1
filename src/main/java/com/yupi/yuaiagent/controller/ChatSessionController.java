package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.model.chat.ChatMessageDTO;
import com.yupi.yuaiagent.model.chat.ChatSessionDTO;
import com.yupi.yuaiagent.service.ChatSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat/session")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @GetMapping("/list")
    public BaseResponse<List<ChatSessionDTO>> list(@RequestParam Long userId,
                                                   @RequestParam String role) {
        return BaseResponse.success(chatSessionService.listMySessions(userId, role));
    }

    @GetMapping("/messages")
    public BaseResponse<List<ChatMessageDTO>> messages(@RequestParam String chatId,
                                                       @RequestParam Long userId) {
        return BaseResponse.success(chatSessionService.listMessages(chatId, userId));
    }
}
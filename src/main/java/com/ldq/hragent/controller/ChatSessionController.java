package com.ldq.hragent.controller;

import com.ldq.hragent.model.auth.LoginUser;
import com.ldq.hragent.model.chat.ChatMessageDTO;
import com.ldq.hragent.model.chat.ChatSessionDTO;
import com.ldq.hragent.service.ChatSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    /**
     * 查询当前登录用户的历史会话列表。
     */
    @GetMapping("/sessions")
    public Map<String, Object> listMySessions(HttpSession session) {
        LoginUser loginUser = getLoginUser(session);

        List<ChatSessionDTO> sessions = chatSessionService.listMySessions(
                loginUser.getUserId(),
                loginUser.getRole().name()
        );

        return success(sessions);
    }

    /**
     * 查询当前登录用户某个会话下的消息。
     */
    @GetMapping("/sessions/{chatId}/messages")
    public Map<String, Object> listMessages(@PathVariable String chatId, HttpSession session) {
        LoginUser loginUser = getLoginUser(session);

        List<ChatMessageDTO> messages = chatSessionService.listMessages(
                chatId,
                loginUser.getUserId(),
                loginUser.getRole().name()
        );

        return success(messages);
    }

    private LoginUser getLoginUser(HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.LOGIN_USER_SESSION_KEY);
        if (loginUser == null) {
            throw new IllegalStateException("请先登录");
        }
        return loginUser;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        result.put("data", data);
        return result;
    }
}
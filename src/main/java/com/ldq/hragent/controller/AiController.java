package com.ldq.hragent.controller;

import com.ldq.hragent.app.HrAgentApp;
import com.ldq.hragent.model.auth.LoginUser;
import com.ldq.hragent.model.dto.ChatRequest;
import com.ldq.hragent.model.enums.ChatRole;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private HrAgentApp hrAgentApp;

    @PostMapping("/employee/chat/sync")
    public String employeeChatSync(@RequestBody @Valid ChatRequest request, HttpSession session) {
        fillLoginContext(request, session, ChatRole.EMPLOYEE);
        return hrAgentApp.doChat(request);
    }

    @PostMapping("/hr/chat/sync")
    public String hrChatSync(@RequestBody @Valid ChatRequest request, HttpSession session) {
        fillLoginContext(request, session, ChatRole.HR);
        return hrAgentApp.doChat(request);
    }

    @PostMapping("/admin/chat/sync")
    public String adminChatSync(@RequestBody @Valid ChatRequest request, HttpSession session) {
        fillLoginContext(request, session, ChatRole.ADMIN);
        return hrAgentApp.doChat(request);
    }

    @PostMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatSse(@RequestBody @Valid ChatRequest request, HttpSession session) {
        fillLoginContext(request, session, null);
        return hrAgentApp.doChatByStream(request);
    }

    @PostMapping(value = "/chat/server_sent_event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatServerSentEvent(@RequestBody @Valid ChatRequest request,
                                                               HttpSession session) {
        fillLoginContext(request, session, null);
        return hrAgentApp.doChatByStream(request)
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    @PostMapping(value = "/chat/sse_emitter")
    public SseEmitter doChatSseEmitter(@RequestBody @Valid ChatRequest request, HttpSession session) {
        fillLoginContext(request, session, null);

        SseEmitter sseEmitter = new SseEmitter(180000L);
        hrAgentApp.doChatByStream(request)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);

        return sseEmitter;
    }

    private void fillLoginContext(ChatRequest request, HttpSession session, ChatRole requiredRole) {
        LoginUser loginUser = (LoginUser) session.getAttribute(AuthController.LOGIN_USER_SESSION_KEY);
        if (loginUser == null) {
            throw new IllegalStateException("请先登录");
        }

        if (requiredRole != null && loginUser.getRole() != requiredRole) {
            throw new IllegalStateException("当前身份无权访问该 AI 入口");
        }

        request.setUserId(loginUser.getUserId());
        request.setRole(loginUser.getRole());

        if (request.getTenantId() == null || request.getTenantId().isBlank()) {
            request.setTenantId("default");
        }
    }
}
package com.ldq.hragent.controller;

import com.ldq.hragent.common.BaseResponse;
import com.ldq.hragent.context.HrRequestContext;
import com.ldq.hragent.context.HrRequestContextHolder;
import com.ldq.hragent.exception.BizException;
import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.service.NotificationPublishService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification/publish")
public class NotificationPublishController {

    private final NotificationPublishService notificationPublishService;

    public NotificationPublishController(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    @PostMapping("/all")
    public BaseResponse<String> publishToAll(@RequestBody PublishRequest request,
                                             @RequestParam Long userId,
                                             @RequestParam String role) {
        return withContext(userId, role, "notification-publish-all", () ->
                BaseResponse.success(notificationPublishService.publishToAll(
                        request.getTitle(),
                        request.getContent(),
                        request.getType()
                )));
    }

    @PostMapping("/hr-admin")
    public BaseResponse<String> publishToHrAdmin(@RequestBody PublishRequest request,
                                                 @RequestParam Long userId,
                                                 @RequestParam String role) {
        return withContext(userId, role, "notification-publish-hr-admin", () ->
                BaseResponse.success(notificationPublishService.publishToHrAndAdmin(
                        request.getTitle(),
                        request.getContent(),
                        request.getType()
                )));
    }

    private BaseResponse<String> withContext(Long userId, String role, String chatId, java.util.function.Supplier<BaseResponse<String>> action) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId(chatId)
                    .role(parseRole(role))
                    .build());
            return action.get();
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    private ChatRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BizException("role 不能为空");
        }
        try {
            return ChatRole.valueOf(role.trim().toUpperCase());
        } catch (Exception e) {
            throw new BizException("role 仅支持 EMPLOYEE / HR / ADMIN");
        }
    }

    public static class PublishRequest {
        private String title;
        private String content;
        private String type = "SYSTEM";

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
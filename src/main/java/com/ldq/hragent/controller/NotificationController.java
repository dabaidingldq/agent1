package com.ldq.hragent.controller;

import com.ldq.hragent.common.BaseResponse;
import com.ldq.hragent.context.HrRequestContext;
import com.ldq.hragent.context.HrRequestContextHolder;
import com.ldq.hragent.exception.BizException;
import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.model.hr.NotificationDTO;
import com.ldq.hragent.model.hr.NotificationStatsResult;
import com.ldq.hragent.service.NotificationQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping("/my")
    public BaseResponse<List<NotificationDTO>> myNotifications(@RequestParam Long userId,
                                                               @RequestParam String role,
                                                               @RequestParam(defaultValue = "10") Integer limit) {
        return withContext(userId, role, "notification-query",
                () -> BaseResponse.success(notificationQueryService.queryMyNotifications(limit)));
    }

    @GetMapping("/stats")
    public BaseResponse<NotificationStatsResult> myStats(@RequestParam Long userId,
                                                         @RequestParam String role) {
        return withContext(userId, role, "notification-stats",
                () -> BaseResponse.success(notificationQueryService.queryMyNotificationStats()));
    }

    @PostMapping("/{id}/read")
    public BaseResponse<String> markAsRead(@PathVariable("id") Long id,
                                           @RequestParam Long userId,
                                           @RequestParam String role) {
        return withContext(userId, role, "notification-read",
                () -> BaseResponse.success(notificationQueryService.markAsRead(id)));
    }

    @PostMapping("/read-all")
    public BaseResponse<String> markAllAsRead(@RequestParam Long userId,
                                              @RequestParam String role) {
        return withContext(userId, role, "notification-read-all",
                () -> BaseResponse.success(notificationQueryService.markAllAsRead()));
    }

    private <T> BaseResponse<T> withContext(Long userId,
                                            String role,
                                            String chatId,
                                            Supplier<BaseResponse<T>> action) {
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
}
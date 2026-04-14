package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.model.hr.NotificationDTO;
import com.yupi.yuaiagent.model.hr.NotificationStatsResult;
import com.yupi.yuaiagent.service.NotificationQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping("/my")
    public BaseResponse<List<NotificationDTO>> myNotifications(@RequestParam Long userId,
                                                               @RequestParam(defaultValue = "10") Integer limit) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("notification-query")
                    .role(ChatRole.EMPLOYEE)
                    .build());
            return BaseResponse.success(notificationQueryService.queryMyNotifications(limit));
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @GetMapping("/stats")
    public BaseResponse<NotificationStatsResult> myStats(@RequestParam Long userId) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("notification-stats")
                    .role(ChatRole.EMPLOYEE)
                    .build());
            return BaseResponse.success(notificationQueryService.queryMyNotificationStats());
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @PostMapping("/{id}/read")
    public BaseResponse<String> markAsRead(@PathVariable("id") Long id,
                                           @RequestParam Long userId) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("notification-read")
                    .role(ChatRole.EMPLOYEE)
                    .build());
            return BaseResponse.success(notificationQueryService.markAsRead(id));
        } finally {
            HrRequestContextHolder.clear();
        }
    }

    @PostMapping("/read-all")
    public BaseResponse<String> markAllAsRead(@RequestParam Long userId) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("notification-read-all")
                    .role(ChatRole.EMPLOYEE)
                    .build());
            return BaseResponse.success(notificationQueryService.markAllAsRead());
        } finally {
            HrRequestContextHolder.clear();
        }
    }
}
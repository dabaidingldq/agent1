package com.ldq.hragent.controller;

import com.ldq.hragent.common.BaseResponse;
import com.ldq.hragent.context.HrRequestContext;
import com.ldq.hragent.context.HrRequestContextHolder;
import com.ldq.hragent.model.enums.ChatRole;
import com.ldq.hragent.model.hr.TeamOvertimeStatResult;
import com.ldq.hragent.service.TeamAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hr/analytics")
public class HrAnalyticsController {

    private final TeamAnalyticsService teamAnalyticsService;

    public HrAnalyticsController(TeamAnalyticsService teamAnalyticsService) {
        this.teamAnalyticsService = teamAnalyticsService;
    }

    @GetMapping("/overtime-top")
    public BaseResponse<List<TeamOvertimeStatResult>> overtimeTop(@RequestParam Long userId,
                                                                  @RequestParam(defaultValue = "5") Integer limit) {
        try {
            HrRequestContextHolder.setContext(HrRequestContext.builder()
                    .userId(userId)
                    .tenantId("default")
                    .chatId("hr-overtime-top")
                    .role(ChatRole.HR)
                    .build());
            return BaseResponse.success(
                    teamAnalyticsService.queryWeeklyTopOvertimeEmployees(limit == null ? 5 : limit)
            );
        } finally {
            HrRequestContextHolder.clear();
        }
    }
}
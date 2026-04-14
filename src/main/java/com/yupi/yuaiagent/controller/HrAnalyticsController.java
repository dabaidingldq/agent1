package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.common.BaseResponse;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.enums.ChatRole;
import com.yupi.yuaiagent.model.hr.TeamOvertimeStatResult;
import com.yupi.yuaiagent.service.TeamAnalyticsService;
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
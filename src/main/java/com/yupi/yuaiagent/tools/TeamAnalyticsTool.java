package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.TeamOvertimeStatResult;
import com.yupi.yuaiagent.service.TeamAnalyticsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamAnalyticsTool {

    private final TeamAnalyticsService teamAnalyticsService;

    public TeamAnalyticsTool(TeamAnalyticsService teamAnalyticsService) {
        this.teamAnalyticsService = teamAnalyticsService;
    }

    @Tool(description = "查询本周团队加班时长最多的员工列表，仅 HR 或管理员可用")
    public List<TeamOvertimeStatResult> queryWeeklyTopOvertimeEmployees(Integer limit) {
        int actualLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
        return teamAnalyticsService.queryWeeklyTopOvertimeEmployees(actualLimit);
    }
}
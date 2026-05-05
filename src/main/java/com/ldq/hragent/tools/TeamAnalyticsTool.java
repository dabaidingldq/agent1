package com.ldq.hragent.tools;

import com.ldq.hragent.model.hr.TeamOvertimeStatResult;
import com.ldq.hragent.service.TeamAnalyticsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamAnalyticsTool {

    private final TeamAnalyticsService teamAnalyticsService;

    public TeamAnalyticsTool(TeamAnalyticsService teamAnalyticsService) {
        this.teamAnalyticsService = teamAnalyticsService;
    }

    @Tool(description = """
            查询本周团队加班时长最多的员工列表，仅 HR 或管理员可用。
            适用场景：
            - HR 或管理员说“查看本周加班最多的人”
            - HR 或管理员说“团队加班排行”
            - HR 或管理员说“加班 Top5 / Top10”
            
            参数规则：
            - limit：返回条数
            - 如果用户没有指定条数，默认 5
            - 最大不超过 20
            - 普通员工不应查询团队统计
            - 返回后应整理成排行列表，不要直接输出原始对象
            """)
    public List<TeamOvertimeStatResult> queryWeeklyTopOvertimeEmployees(Integer limit) {
        int actualLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
        return teamAnalyticsService.queryWeeklyTopOvertimeEmployees(actualLimit);
    }
}
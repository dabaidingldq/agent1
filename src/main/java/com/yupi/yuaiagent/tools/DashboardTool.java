package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.PersonalDashboardResult;
import com.yupi.yuaiagent.service.DashboardService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DashboardTool {

    private final DashboardService dashboardService;

    public DashboardTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Tool(description = "查询当前登录员工的个人数据看板，包括年假使用率、本月加班时长、待审批数量")
    public PersonalDashboardResult queryMyDashboard() {
        return dashboardService.queryMyDashboard();
    }
}
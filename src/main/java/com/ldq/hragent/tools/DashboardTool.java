package com.ldq.hragent.tools;

import com.ldq.hragent.model.hr.PersonalDashboardResult;
import com.ldq.hragent.service.DashboardService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DashboardTool {

    private final DashboardService dashboardService;

    public DashboardTool(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Tool(description = """
            查询当前登录员工的个人数据看板。
            适用场景：
            - 用户说“查看我的个人看板”
            - 用户说“我的年假使用率、本月加班、待审批数量”
            - 用户想了解自己的 HR 相关数据概览
            
            返回内容通常包括：
            - 年假使用率
            - 本月加班时长
            - 待审批数量
            
            权限规则：
            - 只查询当前登录用户本人
            - 不允许查询其他员工个人看板
            - 如果 HR 或管理员想看团队统计，应优先使用 TeamAnalyticsTool
            """)
    public PersonalDashboardResult queryMyDashboard() {
        return dashboardService.queryMyDashboard();
    }
}
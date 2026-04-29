package com.yupi.yuaiagent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistration {

    /**
     * 员工端工具：
     * 只保留“个人事务 + 本人审批 + 本人数据”能力。
     */
    @Bean("employeeTools")
    public ToolCallback[] employeeTools(LeaveTool leaveTool,
                                        OfficeSupplyTool officeSupplyTool,
                                        ApprovalTool approvalTool,
                                        ApprovalHistoryTool approvalHistoryTool,
                                        ProfileTool profileTool,
                                        CertificateTool certificateTool,
                                        DashboardTool dashboardTool,
                                        ExpenseTool expenseTool,
                                        MeetingRoomTool meetingRoomTool,
                                        VisitorTool visitorTool) {
        return ToolCallbacks.from(
                leaveTool,
                officeSupplyTool,
                approvalTool,
                approvalHistoryTool,
                profileTool,
                certificateTool,
                dashboardTool,
                expenseTool,
                meetingRoomTool,
                visitorTool
        );
    }

    /**
     * HR 端工具：
     * HR 可以处理员工事务、审批历史、公告、团队分析等。
     */
    @Bean("hrTools")
    public ToolCallback[] hrTools(LeaveTool leaveTool,
                                  ApprovalTool approvalTool,
                                  ApprovalHistoryTool approvalHistoryTool,
                                  CertificateTool certificateTool,
                                  AnnouncementTool announcementTool,
                                  DashboardTool dashboardTool,
                                  TeamAnalyticsTool teamAnalyticsTool,
                                  ExpenseTool expenseTool,
                                  MeetingRoomTool meetingRoomTool,
                                  VisitorTool visitorTool) {
        return ToolCallbacks.from(
                leaveTool,
                approvalTool,
                approvalHistoryTool,
                certificateTool,
                announcementTool,
                dashboardTool,
                teamAnalyticsTool,
                expenseTool,
                meetingRoomTool,
                visitorTool
        );
    }

    /**
     * 管理员端工具：
     * 管理员偏系统治理、知识库、预算、全局审批和团队统计。
     */
    @Bean("adminTools")
    public ToolCallback[] adminTools(ApprovalTool approvalTool,
                                     ApprovalHistoryTool approvalHistoryTool,
                                     DashboardTool dashboardTool,
                                     AnnouncementTool announcementTool,
                                     KnowledgeAdminTool knowledgeAdminTool,
                                     BudgetTool budgetTool,
                                     TeamAnalyticsTool teamAnalyticsTool) {
        return ToolCallbacks.from(
                approvalTool,
                approvalHistoryTool,
                dashboardTool,
                announcementTool,
                knowledgeAdminTool,
                budgetTool,
                teamAnalyticsTool
        );
    }
}
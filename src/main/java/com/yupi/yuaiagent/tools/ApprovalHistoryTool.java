package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.ApprovalHistoryResult;
import com.yupi.yuaiagent.service.ApprovalHistoryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApprovalHistoryTool {

    private final ApprovalHistoryService approvalHistoryService;

    public ApprovalHistoryTool(ApprovalHistoryService approvalHistoryService) {
        this.approvalHistoryService = approvalHistoryService;
    }

    @Tool(description = "查询当前登录用户自己的历史审批记录。month 格式为 yyyy-MM，例如 2026-04")
    public List<ApprovalHistoryResult> queryMyApprovalHistory(String businessType, String month) {
        return approvalHistoryService.queryMyApprovalHistory(businessType, month);
    }

    @Tool(description = "查询团队或全局历史审批记录，仅团队负责人、HR 或管理员可用。month 格式为 yyyy-MM")
    public List<ApprovalHistoryResult> queryTeamApprovalHistory(String businessType, String month) {
        return approvalHistoryService.queryTeamApprovalHistory(businessType, month);
    }
}
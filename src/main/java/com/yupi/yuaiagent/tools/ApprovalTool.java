package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.ApprovalProgressResult;
import com.yupi.yuaiagent.service.ApprovalService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ApprovalTool {

    private final ApprovalService approvalService;

    public ApprovalTool(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Tool(description = "查询当前登录用户最近一条审批进度。businessType 可选，例如 LEAVE、OFFICE_SUPPLY、CERTIFICATE")
    public ApprovalProgressResult queryLatestMyApproval(String businessType) {
        return approvalService.queryLatestMyApproval(businessType);
    }

    @Tool(description = "对当前登录用户本人发起的审批单发送催办提醒")
    public String remindCurrentApprover(Long approvalId) {
        return approvalService.remindCurrentApprover(approvalId);
    }
}
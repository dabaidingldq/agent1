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

    @Tool(description = """
            查询当前登录用户最近一条审批进度。
            适用场景：
            - 用户说“我的审批进度”
            - 用户说“最近一条审批”
            - 用户说“我提交的请假到哪了”
            - 用户说“办公用品审批状态”
            - 用户说“证明开具审批进度”
            
            注意：
            - 这是查询本人发起的最近审批进度，不是查询团队审批历史
            - 如果用户要查团队或全局历史审批，应使用审批历史工具
            
            参数规则：
            - businessType 可选：LEAVE、OFFICE_SUPPLY、CERTIFICATE、EXPENSE
            - 请假=LEAVE
            - 办公用品=OFFICE_SUPPLY
            - 证明/在职证明=CERTIFICATE
            - 报销=EXPENSE
            - 如果用户没有指定类型，businessType 传空字符串
            """)
    public ApprovalProgressResult queryLatestMyApproval(String businessType) {
        return approvalService.queryLatestMyApproval(normalizeBusinessType(businessType));
    }

    @Tool(description = """
            对当前登录用户本人发起的审批单发送催办提醒。
            适用场景：
            - 用户说“帮我催一下审批”
            - 用户说“催办这个审批单”
            - 用户说“提醒当前审批人”
            
            参数规则：
            - approvalId 必须是具体审批单 ID
            - 只能催办当前登录用户本人发起的审批单
            - 如果没有具体审批单 ID，应先查询最近一条审批进度，再根据结果判断是否可催办
            """)
    public String remindCurrentApprover(Long approvalId) {
        return approvalService.remindCurrentApprover(approvalId);
    }

    private String normalizeBusinessType(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return "";
        }

        String text = businessType.trim().toUpperCase();

        if (text.contains("请假") || text.contains("LEAVE")) {
            return "LEAVE";
        }
        if (text.contains("办公") || text.contains("OFFICE")) {
            return "OFFICE_SUPPLY";
        }
        if (text.contains("证明") || text.contains("CERT")) {
            return "CERTIFICATE";
        }
        if (text.contains("报销") || text.contains("EXPENSE")) {
            return "EXPENSE";
        }

        return text;
    }
}
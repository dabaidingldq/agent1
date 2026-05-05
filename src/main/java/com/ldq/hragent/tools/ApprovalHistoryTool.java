package com.ldq.hragent.tools;

import com.ldq.hragent.model.hr.ApprovalHistoryResult;
import com.ldq.hragent.service.ApprovalHistoryService;
import com.ldq.hragent.utils.DateContextUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApprovalHistoryTool {

    private final ApprovalHistoryService approvalHistoryService;

    public ApprovalHistoryTool(ApprovalHistoryService approvalHistoryService) {
        this.approvalHistoryService = approvalHistoryService;
    }

    @Tool(description = """
            查询当前登录用户自己发起的历史审批记录。
            适用场景：
            - 普通员工说“我的历史审批记录”
            - 用户说“我提交过的请假审批”
            - 用户说“我的办公用品审批历史”
            - 用户说“我最近的审批历史”
            
            参数规则：
            - businessType 可选值：LEAVE、OFFICE_SUPPLY、CERTIFICATE、EXPENSE
            - 用户说“请假”时 businessType=LEAVE
            - 用户说“办公用品”时 businessType=OFFICE_SUPPLY
            - 用户说“证明 / 在职证明 / 证明开具”时 businessType=CERTIFICATE
            - 用户说“报销”时 businessType=EXPENSE
            - 如果用户没有指定业务类型，businessType 传空字符串
            - month 必须是 yyyy-MM 格式
            - 用户说“本月 / 这个月 / 当前月 / 最近”时，month 可以传空字符串，工具内部会自动转换为当前系统月份
            - 用户说“今年4月”时，工具内部会自动转换为当前年份的 04 月
            - 用户说“2026年4月”时，工具内部会自动转换为 2026-04
            - 如果没有月份，month 传空字符串
            """)
    public List<ApprovalHistoryResult> queryMyApprovalHistory(String businessType, String month) {
        return approvalHistoryService.queryMyApprovalHistory(
                normalizeBusinessType(businessType),
                DateContextUtils.normalizeMonth(month)
        );
    }

    @Tool(description = """
            查询团队或全局历史审批记录，仅团队负责人、HR 或管理员可用。
            适用场景：
            - HR 或管理员说“历史审批记录”
            - HR 或管理员说“团队审批历史”
            - HR 或管理员说“全局审批记录”
            - HR 或管理员说“请假的历史审批”
            - HR 或管理员说“办公用品历史审批”
            - HR 或管理员说“最近的历史审批记录”
            
            权限规则：
            - HR 可查询团队或全局审批历史
            - 管理员可查询全部审批历史
            - 普通员工不能越权查询他人审批历史
            
            参数规则：
            - businessType 可选值：LEAVE、OFFICE_SUPPLY、CERTIFICATE、EXPENSE
            - 用户说“请假”时 businessType=LEAVE
            - 用户说“办公用品”时 businessType=OFFICE_SUPPLY
            - 用户说“证明 / 在职证明 / 证明开具”时 businessType=CERTIFICATE
            - 用户说“报销”时 businessType=EXPENSE
            - 如果用户没有指定业务类型，businessType 传空字符串
            - month 必须是 yyyy-MM 格式
            - 用户说“本月 / 这个月 / 当前月 / 最近”时，month 可以传空字符串，工具内部会自动转换为当前系统月份
            - 用户说“今年4月”时，工具内部会自动转换为当前年份的 04 月
            - 用户说“2026年4月”时，工具内部会自动转换为 2026-04
            - 如果没有月份，month 传空字符串
            - 如果用户已经给出业务类型和月份，必须直接调用本工具，不要继续追问
            """)
    public List<ApprovalHistoryResult> queryTeamApprovalHistory(String businessType, String month) {
        return approvalHistoryService.queryTeamApprovalHistory(
                normalizeBusinessType(businessType),
                DateContextUtils.normalizeMonth(month)
        );
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
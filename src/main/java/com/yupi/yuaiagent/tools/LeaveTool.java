package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.LeaveBalanceResult;
import com.yupi.yuaiagent.model.hr.LeaveRequestResult;
import com.yupi.yuaiagent.model.hr.LeaveValidationResult;
import com.yupi.yuaiagent.service.LeaveService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class LeaveTool {

    private final LeaveService leaveService;

    public LeaveTool(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @Tool(description = """
            查询当前登录员工的休假余额。
            适用场景：
            - 用户说“查一下我的年假”
            - 用户说“我还有多少年假”
            - 用户说“查询休假余额 / 请假余额 / 假期余额”
            - 用户询问年假、病假、事假、调休等剩余额度
            
            调用规则：
            - 只查询当前登录用户本人
            - 不需要用户额外提供 userId
            - 不允许查询他人的休假余额
            - 如果用户问的是政策解释，例如“年假政策是什么”，应优先查知识库，不应调用本工具
            """)
    public LeaveBalanceResult queryMyLeaveBalance() {
        return leaveService.queryMyLeaveBalance();
    }

    @Tool(description = """
            校验当前登录员工的请假申请是否存在时间冲突或额度不足。
            适用场景：
            - 用户说“我想请假，先帮我看看能不能请”
            - 用户提供了请假类型、开始时间、结束时间，希望判断是否可提交
            - 用户不确定请假时间是否冲突或额度是否够
            
            参数规则：
            - leaveType：请假类型，例如 年假、病假、事假、调休
            - startTime：开始时间，必须尽量转换为 ISO-8601 日期时间格式，例如 2026-04-17T09:00:00
            - endTime：结束时间，必须尽量转换为 ISO-8601 日期时间格式，例如 2026-04-17T18:00:00
            - 如果用户缺少开始时间、结束时间或请假类型，不要调用工具，应先追问缺失字段
            """)
    public LeaveValidationResult validateLeaveRequest(String leaveType, String startTime, String endTime) {
        return leaveService.validateLeaveRequest(leaveType, startTime, endTime);
    }

    @Tool(description = """
            为当前登录员工创建请假申请。
            适用场景：
            - 用户明确说“帮我提交请假申请”
            - 用户已经提供请假类型、开始时间、结束时间、请假原因
            - 用户通过快捷入口提交了请假表单
            
            参数规则：
            - leaveType：请假类型，例如 年假、病假、事假、调休
            - startTime：开始时间，使用 ISO-8601 日期时间格式，例如 2026-04-17T09:00:00
            - endTime：结束时间，使用 ISO-8601 日期时间格式，例如 2026-04-17T18:00:00
            - reason：请假原因
            - 如果缺少必要字段，不要编造，先追问
            - 创建前如果用户只是说“看看能不能请”，应调用 validateLeaveRequest，不应直接创建
            """)
    public LeaveRequestResult createLeaveRequest(String leaveType, String startTime, String endTime, String reason) {
        return leaveService.createLeaveRequest(leaveType, startTime, endTime, reason);
    }
}
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

    @Tool(description = "查询当前登录员工的年假、事假、病假、调休等休假余额")
    public LeaveBalanceResult queryMyLeaveBalance() {
        return leaveService.queryMyLeaveBalance();
    }

    @Tool(description = "校验当前登录员工的请假申请是否存在时间冲突或额度不足。startTime 和 endTime 使用 ISO-8601 日期时间格式")
    public LeaveValidationResult validateLeaveRequest(String leaveType, String startTime, String endTime) {
        return leaveService.validateLeaveRequest(leaveType, startTime, endTime);
    }

    @Tool(description = "为当前登录员工创建请假申请。startTime 和 endTime 使用 ISO-8601 日期时间格式，例如 2026-04-17T13:00:00")
    public LeaveRequestResult createLeaveRequest(String leaveType, String startTime, String endTime, String reason) {
        return leaveService.createLeaveRequest(leaveType, startTime, endTime, reason);
    }
}
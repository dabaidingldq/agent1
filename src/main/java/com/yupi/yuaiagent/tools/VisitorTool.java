package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.VisitorRequestResult;
import com.yupi.yuaiagent.service.VisitorService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class VisitorTool {

    private final VisitorService visitorService;

    public VisitorTool(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Tool(description = "为当前登录员工创建访客预约申请。visitDate 使用 yyyy-MM-dd 格式")
    public VisitorRequestResult createVisitorRequest(String visitorName, String mobile, String visitDate, String visitReason) {
        return visitorService.createVisitorRequest(visitorName, mobile, visitDate, visitReason);
    }
}
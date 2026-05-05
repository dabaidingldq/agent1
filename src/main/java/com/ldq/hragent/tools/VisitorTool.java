package com.ldq.hragent.tools;

import com.ldq.hragent.model.hr.VisitorRequestResult;
import com.ldq.hragent.service.VisitorService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class VisitorTool {

    private final VisitorService visitorService;

    public VisitorTool(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Tool(description = """
            为当前登录员工创建访客预约申请。
            适用场景：
            - 用户说“我要预约访客”
            - 用户说“明天有客户来访，帮我登记”
            - 用户通过快捷入口提交访客申请
            
            参数规则：
            - visitorName：访客姓名
            - mobile：访客手机号。如果用户未提供，可以传空字符串，但应提示可能需要补充
            - visitDate：来访日期，使用 yyyy-MM-dd 格式，例如 2026-04-20
            - visitReason：来访原因或目的
            - 如果缺少访客姓名、来访日期或来访原因，应先追问
            - 不要编造访客联系方式
            """)
    public VisitorRequestResult createVisitorRequest(String visitorName, String mobile, String visitDate, String visitReason) {
        return visitorService.createVisitorRequest(visitorName, mobile, visitDate, visitReason);
    }
}
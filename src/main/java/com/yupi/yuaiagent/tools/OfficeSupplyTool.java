package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.OfficeSupplyApplyResult;
import com.yupi.yuaiagent.service.OfficeSupplyService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OfficeSupplyTool {

    private final OfficeSupplyService officeSupplyService;

    public OfficeSupplyTool(OfficeSupplyService officeSupplyService) {
        this.officeSupplyService = officeSupplyService;
    }

    @Tool(description = "为当前登录员工创建办公用品申请。items 是用品清单描述，例如 两支黑色签字笔和一个笔记本")
    public OfficeSupplyApplyResult applyOfficeSupplies(String items) {
        return officeSupplyService.applyOfficeSupplies(items);
    }
}